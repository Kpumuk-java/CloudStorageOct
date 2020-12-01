package netty.handlers;

import info.FileInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ServerNetty extends SimpleChannelInboundHandler<ByteBuf> {

    private Path directPath = Paths.get("server/");
    private String[] pathCommand1;
    private List<Path> pathCommand = new ArrayList<>(2);
    private List<FileInfo> list;
    private int POSITION_WRITE_FILE = 0;
    private boolean DOWNLOAD_FILE = false;
    private boolean auth = false;
    private byte[] acceptByte;
    // TODO: 27.11.2020 Сделать нормальную авторизацию через бд
    private Map<String, String> authList = new HashMap<String, String>() {{
        put("login", "123");
        put("login2", "456");
    }};

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");

    }

    private byte[] convertToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        System.out.println("Message from client: " + byteBuf.toString());

        if (!auth) {
            checkAuth(ctx, getByteArray(byteBuf, 0));
            return;
        } else {
            pathCommand1 = null;
            System.out.println(byteBuf.getByte(0));

            if (byteBuf.getByte(0) == '#' || DOWNLOAD_FILE) {
                getByteFromTheBuffer(byteBuf, 0);
                return;
            }

            //copy Client -> Server
            if (byteBuf.getByte(0) == 11) {
                pathCommand.clear();
                int sizeFile;
                acceptByte = new byte[4];
                for (int i = 0; i < 4; i++) {
                    acceptByte[i] = byteBuf.getByte(i + 1);
                }
                sizeFile = ByteBuffer.wrap(acceptByte).getInt();
                System.out.println(sizeFile);
                getPaths(getByteArray(byteBuf, 5));
                acceptByte = new byte[sizeFile];
                DOWNLOAD_FILE = true;
                System.out.println("размер принимаемого файла " + acceptByte.length);
            }

            // continue Client -> Server


            if (byteBuf.getByte(0) == 99) {
                System.out.println("close");
                ctx.close();
            }
            // ls
            if (byteBuf.getByte(0) == 1) {
                System.out.println("ls");
                byte[] sendByte = convertToBytes(getFilesList());
                System.out.println(sendByte.length);
                ByteBuf btf = ctx.alloc().buffer().writeBytes(sendByte);
                System.out.println(getFilesList().toString());
                ctx.writeAndFlush(sizeMsg(ctx, sendByte.length));
                ctx.writeAndFlush(btf);
            }
            // up
            if (byteBuf.getByte(0) == 2) {
                System.out.println("up");
                directPath = directPath.getParent();
                System.out.println(directPath.toString());
            }
            // path
            if (byteBuf.getByte(0) == 3) {
                System.out.println("path");
                ctx.writeAndFlush(sizeMsg(ctx, convertToBytes(directPath.toString()).length));
                ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes(directPath.toString())));
            }
            // delete
            if (byteBuf.getByte(0) == 4) {
                System.out.println("delete");
                getPaths(getByteArray(byteBuf, 1));
                Files.delete(pathCommand.get(0));
            }

            // upload
            if (byteBuf.getByte(0) == 5) {
                System.out.println("upload");
                getPaths(getByteArray(byteBuf, 1));
                if (Files.exists(pathCommand.get(0))) {
                    System.out.println("file exists");
                    ctx.writeAndFlush(sizeMsg(ctx, (int) Files.size(pathCommand.get(0))));
                    byte[] sendByte = Files.readAllBytes(pathCommand.get(0));
                    ByteBuf btf = ctx.alloc().buffer().writeBytes(sendByte);
                    ctx.writeAndFlush(btf);
                    System.out.println("upload OK");
                } else {
                    System.out.println(Files.exists(pathCommand.get(0)));
                }
            }

            // copyServer
            if (byteBuf.getByte(0) == 6) {
                getPaths(getByteArray(byteBuf, 1));
                System.out.println("copy server");
                if (Files.exists(pathCommand.get(0))) {
                    System.out.println("file exists");
                    Files.copy(pathCommand.get(0), pathCommand.get(1));
                } else {
                    System.out.println(Files.exists(pathCommand.get(0)));
                }


            }
            // cd
            if (byteBuf.getByte(0) == 7) {
                System.out.println("cd");
                getPaths(getByteArray(byteBuf, 1));
                System.out.println("количество элементов в листе путей " + pathCommand.size() + "\n" + pathCommand.toString());
                if (Files.exists(pathCommand.get(0))) {
                    directPath = pathCommand.get(0);
                    System.out.println(directPath);
                } else {
                    // TODO: 09.11.2020 вывод обратно текст с ошибкой
                    System.out.println("WRONG");
                }
            }
            // touch
            if (byteBuf.getByte(0) == 8) {
                System.out.println("touch");
                String[] touch = pathCommand1[1].split("/");
                if (!Files.exists(Paths.get(pathCommand1[1]))) {
                    createFile(touch);
                } else {
                    ctx.writeAndFlush("Don't create file");
                }
            }
        }


    }

    private void getByteFromTheBuffer(ByteBuf byteBuf, int start) {
        try {
            for (int i = start; i < byteBuf.writerIndex(); i++) {
                //if (!(byteBuf.getByte(i) == '#')) {
                    acceptByte[POSITION_WRITE_FILE++] = byteBuf.getByte(i);
                    if (POSITION_WRITE_FILE == acceptByte.length) {
                        DOWNLOAD_FILE = false;
                        System.out.println("Позиция " + POSITION_WRITE_FILE);
                        if (!Files.exists(pathCommand.get(0))) {
                            try {
                                Files.createFile(pathCommand.get(0));
                            } catch (IOException e) {
                                System.out.println("Не удалось создать файл");
                            }
                        }
                        Files.write(pathCommand.get(0), acceptByte);
                        POSITION_WRITE_FILE = 0;
                    }
                //}
            }
            System.out.println("Передано байт " + POSITION_WRITE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public byte[] getByteArray(ByteBuf buf, int positionRead) {
        int pos = 0;
        System.out.println(buf.toString());
        System.out.println(buf.writerIndex());
        byte[] b = new byte[buf.writerIndex()];
        for (int i = positionRead; i < buf.writerIndex(); i++) {
            b[pos++] = buf.getByte(i);
        }
        System.out.println(b.length);
        return b;
    }

    private void getPaths(byte[] array) {
        pathCommand.clear();
        int startPath = 0;
        for (int i = 0; i < array.length - 2; i++) {
            if (array[i] == (byte) '|') {
                pathCommand.add(Paths.get(new String(Arrays.copyOfRange(array, startPath, i))));
                startPath = i + 1;
                System.out.println(pathCommand.get(0));
            }
            if (i == array.length - 3) {
                System.out.println(array.length + " " + i + " " + new String(Arrays.copyOfRange(array, startPath, array.length - 1)));
                pathCommand.add(Paths.get(new String(Arrays.copyOfRange(array, startPath, array.length - 1)).trim()));
                System.out.println(new String(array));
            }
        }

    }

    private void checkAuth(ChannelHandlerContext ctx, byte[] registration) throws IOException {

        for (int i = 0; i < registration.length; i++) {
            if (registration[i] == '|') {
                Iterator<Map.Entry<String, String>> iterator = authList.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    System.out.println(new String(Arrays.copyOfRange(registration, 0, i)));
                    System.out.println(new String(Arrays.copyOfRange(registration, i + 1, registration.length)).trim());
                    if (entry.getKey().equals(new String(Arrays.copyOfRange(registration, 0, i))) &&
                            entry.getValue().equals(new String(Arrays.copyOfRange(registration, i + 1, registration.length)).trim())) {
                        System.out.println("login and password correctly");
                        auth = true;
                        ctx.writeAndFlush(sizeMsg(ctx, convertToBytes("OK").length));
                        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes("OK")));
                        return;
                    }
                    System.out.println(i + " " + entry.getKey() + " " + entry.getValue());
                }
            }
        }

        ctx.writeAndFlush(sizeMsg(ctx, convertToBytes("FALSE").length));
        ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes("FALSE")));


    }


    private ByteBuf sizeMsg(ChannelHandlerContext ctx, int size) {
        return ctx.alloc().buffer().writeBytes(ByteBuffer.allocate(4).putInt(size).array());
    }

    private boolean validPath(String s) {
        if (s.split("\\.").length > 1) {
            return false;
        }
        return true;
    }

    private void createFile(String[] touch) throws IOException {
        if (touch[touch.length - 1].split("\\.").length > 1) {
            Path pathDirectory = Paths.get(pathCommand1[1].substring(0,
                    pathCommand1[1].length() - touch[touch.length - 1].length()));
            Files.createDirectory(pathDirectory);
            Files.createFile(Paths.get(pathCommand1[1]));
        }
    }

    private List<FileInfo> getFilesList() {
        try {
            list = Files.list(directPath).map(FileInfo::new).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected");
    }

}
