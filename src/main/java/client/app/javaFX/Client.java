package client.app.javaFX;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

public class Client {
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private int SERVER_PORT = 8189;
    private SocketChannel channel;
    private Selector selector;
    private ObjectInputStream in;
    private byte[] inputBufferByte;

    public void connected() {
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress("localhost", SERVER_PORT));
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_CONNECT);

            while (channel.isOpen()) {
                selector.select();
                var selectionKeys = selector.selectedKeys();
                var iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {

                    var key = iterator.next();

                    if (key.isConnectable()) {
                        channel.finishConnect();
                        channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                        return;
                    }
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void msgServer(String s) {
        try {
            channel.write(ByteBuffer.wrap(s.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateListServer() {
        try {
            while (channel.isOpen()) {
                selector.select();
                var selectionKeys = selector.selectedKeys();
                var iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {

                    var key = iterator.next();

                    if (key.isWritable()) {
                        channel.write(ByteBuffer.wrap("ls".getBytes()));
                        Thread.sleep(100);

                        inputBufferByte = inputObject();

                        List<FileInfo> list = (List<FileInfo>) convertFromBytes(inputBufferByte);
                        /*for (FileInfo f : list) {
                            System.out.println("write");
                            System.out.println(f.getFileName());
                        }*/
                        inputBufferByte = null;
                        channel.write(ByteBuffer.wrap("path".getBytes()));
                        Thread.sleep(200);
                        inputBufferByte = inputObject();

                        PanelController.setPathFieldServer((String) convertFromBytes(inputBufferByte));
                        PanelController.setList(list);
                        return;
                    }

                    iterator.remove();
                }

            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] inputObject() throws IOException {

        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return null;
        }
        if (read == 0) {
            return null;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();

        return buf;
    }

    private static Object convertFromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }
}
