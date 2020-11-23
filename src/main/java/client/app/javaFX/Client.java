package client.app.javaFX;

import info.FileInfo;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Client {
    private boolean auth = false;
    private int BUFFER_SIZE = 6000;
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
                Set selectionKeys = selector.selectedKeys();
                Iterator iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {

                    SelectionKey key = (SelectionKey) iterator.next();

                    if (key.isConnectable()) {
                        channel.finishConnect();
                        channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
                        return;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void msgServer(String s) {
        try {
            channel.write(ByteBuffer.wrap(s.getBytes()));
            Thread.sleep(100);
            buffer.clear();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void updateListServer(PanelController panelController) {
        try {
            channel.write(ByteBuffer.wrap("ls".getBytes()));
            buffer.clear();
            inputBufferByte = inputObject();
            if (inputBufferByte != null) {
                System.out.println("пришел List");
                List<FileInfo> list = (List<FileInfo>) convertFromBytes(inputBufferByte);
                System.out.println(list.toString());
                System.out.println();
                panelController.setList(list);
            } else {
                System.out.println("List is empty");
                panelController.setList(null);
            }
            channel.write(ByteBuffer.wrap("path".getBytes()));
            buffer.clear();
            inputBufferByte = inputObject();
            System.out.println((String) convertFromBytes(inputBufferByte));
            panelController.setPathFieldServer((String) convertFromBytes(inputBufferByte));


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authSend(String s) throws IOException {

        try {
            inputBufferByte = null;
            channel.write(ByteBuffer.wrap(s.getBytes()));
            //buffer.flip();
            Thread.sleep(200);
            inputBufferByte = inputObject();
            if (inputBufferByte != null) {
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

    private byte[] inputObject() {
        try {
            System.out.println("Ждем байты");
            int read = 0;
            int pos = 0;
            byte count = 0;
            byte[] buf = new byte[4];
            // TODO: 22.11.2020 Добавить механизм прерывание цикла через 1 минут если нет ответа
            while (read <= 3) {
                Thread.sleep(100);
                read = channel.read(buffer);
                System.out.println("Ждем байты для определения размера посылки");
                if (read > 3) {
                    buffer.flip();
                    for (int i = 0; i < 4; i++) {
                        buf[i] = buffer.get();
                        buffer.position(i + 1);
                    }
                    sizeFile = ByteBuffer.wrap(buf).getInt();
                    buf = new byte[sizeFile];
                    continue;
                }
            }
            System.out.println(sizeFile);
            System.out.println("принимаем посылку");
            while (true) {
                if (count == 0) {
                    while (buffer.hasRemaining()) {
                        buf[pos++] = buffer.get();
                    }
                    if (pos == sizeFile) {
                        return buf;
                    }
                    count++;
                    buffer.clear();
                } else if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        buf[pos++] = buffer.get();
                    }
                    if (pos == sizeFile) {
                        return buf;
                    }
                    buffer.clear();
                }
                read = channel.read(buffer);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        return null;
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
            channel.write(ByteBuffer.wrap("exit".getBytes()));
            selector.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] downloadForServer(String s) {
        byte[] b = null;
        System.out.println(s);
        msgServer(s);
        return b = inputObject();
    }

    public boolean isAuth() {
        return auth;
    }

    public boolean downloadInServer(String s, String fileName, String currentPath) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            buffer.clear();
            buffer.put((byte) 11); // команда на начало передачи файла
            buffer.put(ByteBuffer.allocate(4).putInt((int) Files.size(Paths.get(s).resolve(fileName))).array()); // размер файла
            buffer.put((currentPath + "\\" + fileName).getBytes()); // путь к файлу с именем файла
            channel.write(buffer);
            buffer.clear();
            byte[] sendByte = Files.readAllBytes(Paths.get(s).resolve(fileName));
            System.out.println("размер файла" + sendByte.length);
            int pos = 0, endFile = sendByte.length - 1;
            while (pos == endFile) {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    buffer.put(i, sendByte[pos++]);
                }
                channel.write(buffer);
                buffer.clear();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
