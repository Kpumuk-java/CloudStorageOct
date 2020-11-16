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
    private boolean auth = false;
    private int BUFFER_SIZE = 1024;
    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private int SERVER_PORT = 8189;
    private SocketChannel channel;
    private Selector selector;
    private byte[] inputBufferByte;
    private int sizeFile = BUFFER_SIZE;

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
            buffer.clear();
            //buffer.flip();
            Thread.sleep(100);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateListServer(PanelController panelController) {
        try {

            while (channel.isOpen()) {
                selector.select();
                var selectionKeys = selector.selectedKeys();
                var iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {

                    var key = iterator.next();

                    if (key.isWritable()) {
                        channel.write(ByteBuffer.wrap("ls".getBytes()));
                        Thread.sleep(200);
                        //buffer.flip();
                        //Thread.sleep(100);
                        //inputBufferByte = null;
                        buffer.clear();
                        inputBufferByte = inputObject();
                        //sizeFile = ByteBuffer.wrap(inputBufferByte).getInt() + 1024;
                        //System.out.println(sizeFile);
                        //inputBufferByte = inputObject();
                        if (inputBufferByte != null) {
                            System.out.println("1");
                            List<FileInfo> list = (List<FileInfo>) convertFromBytes(inputBufferByte);
                            panelController.setList(list);
                        /*for (FileInfo f : list) {
                            System.out.println("write");
                            System.out.println(f.getFileName());
                        }*/
                        } else {
                            System.out.println("List is empty");
                            panelController.setList(null);
                        }
                        //inputBufferByte = null;
                        //buffer.flip();
                        channel.write(ByteBuffer.wrap("path".getBytes()));
                        //buffer.flip();
                        Thread.sleep(200);
                        inputBufferByte = inputObject();
                        panelController.setPathFieldServer((String) convertFromBytes(inputBufferByte));

                        return;
                    }

                    iterator.remove();
                }

            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void authSend (String s) throws IOException {

        try {
            inputBufferByte = null;
            channel.write(ByteBuffer.wrap(s.getBytes()));
            //buffer.flip();
            Thread.sleep(200);
            inputBufferByte = inputObject();
            if (inputBufferByte != null ) {
                String authString = (String) convertFromBytes(inputBufferByte);
                if (authString.equals("OK")) {
                    auth = true;
                }
            } else {
                System.out.println("return is empty");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private byte[] inputObject() throws IOException {
        System.out.println("started input byte");

        int read = channel.read(buffer);
            //System.out.println("read channel " + read);
            if (read == -1) {
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
        sizeFile = BUFFER_SIZE;
        return buf;
    }

    private static Object convertFromBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeChannel() {
        try {
            //channel.write(ByteBuffer.wrap("exit".getBytes()));
            selector.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cdServer(String msg) {
    }

    public byte[] downloadForServer(String s) {
        byte[] b = null;
        System.out.println(s);
        try {
            msgServer(s);
            Thread.sleep(100);
            b = inputObject();
            sizeFile = ByteBuffer.wrap(b).getInt();
            System.out.println("download " + sizeFile);
            System.out.println(ByteBuffer.wrap(b).getInt());
            b = inputObject();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return b;
    }

    public boolean isAuth() {
        return auth;
    }
}
