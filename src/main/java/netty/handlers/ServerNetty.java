package netty.handlers;

import client.app.javaFX.FileInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ServerNetty extends SimpleChannelInboundHandler<String> {

    private String rootPath = "/";
    private Path directPath = Path.of("server/");
    private String[] pathCommand;
    private List<FileInfo> list;
    private boolean auth = false;




    /*public static final ConcurrentLinkedDeque<ChannelHandlerContext> channels =
            new ConcurrentLinkedDeque<>();*/


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
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        System.out.println("Message from client: " + s);

        /*if (!auth) {
            checkAuth(ctx, s);
            return;
        }*/

        pathCommand = null;

        if (s.equals("ls")) {
            byte[] sendByte = convertToBytes(getFilesList());
            System.out.println(sendByte.length);
            ByteBuf btf = ctx.alloc().buffer().writeBytes(sendByte);
            System.out.println(getFilesList().toString());
            //ctx.writeAndFlush(sizeMsg(ctx, sendByte.length));
            //System.out.println("flush");
            //Thread.sleep(50);
            ctx.writeAndFlush(btf);
        }

        if (s.equals("up")) {
            directPath = directPath.getParent();
            System.out.println(directPath.toString());
        }

        if (s.equals("path")) {
            ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes(directPath.toString())));
        }

        if (s.split(" ").length > 1) {
            pathCommand = s.split(" ");
            //String[] fragmentPath = pathCommand[1].split("\\\\");

            if (pathCommand[0].equals("delete")) {
                if (Files.exists(Paths.get(pathCommand[1]))) {
                    System.out.println("started deleted " + pathCommand[1]);
                    Files.delete(Paths.get(pathCommand[1]));
                    System.out.println("finished deleted " + pathCommand[1]);
                }
            }

            if (pathCommand[0].equals("download") && pathCommand.length == 2) {
                System.out.println("if file exists");
                if (Files.exists(Paths.get(pathCommand[1]))) {
                    System.out.println("file exists");
                    byte[] sendByte = Files.readAllBytes(Paths.get(pathCommand[1]));
                    ByteBuf btf = ctx.alloc().buffer().writeBytes(sendByte);
                    System.out.println(sendByte.length + " ");
                    System.out.println("download started");
                    //ctx.writeAndFlush(sizeMsg(ctx, sendByte.length));
                    //Thread.sleep(500);
                    ctx.writeAndFlush(btf);
                    System.out.println("download OK");
                }
            }

            if (pathCommand[0].equals(("copyServer")) && pathCommand.length == 4) {
                System.out.println("copy server");
                if (pathCommand[3].equals("rw")) {

                } else if (pathCommand[3].equals("0")) {
                    Files.copy(Paths.get(pathCommand[1]), Paths.get(pathCommand[2]));

                } else {
                    System.out.println("do not creat file");
                }
            }

            if (pathCommand[0].equals("cd") && pathCommand.length > 1) {
                System.out.println(pathCommand[1]);
                if (Files.exists(Path.of(pathCommand[1]))) {
                    System.out.println(pathCommand[1]);
                    directPath = Paths.get(pathCommand[1]);
                } else {
                    // TODO: 09.11.2020 вывод обратно текст с ошибкой
                    System.out.println("WRONG");
                }
            }

            if (pathCommand[0].equals("touch") && s.split(" ").length == 2) {
                String[] touch = pathCommand[1].split("/");
                if (!Files.exists(Path.of(pathCommand[1]))) {
                    createFile(touch);
                } else {
                    ctx.writeAndFlush("Don't create file");
                }
            }

        }
    }

    private void checkAuth(ChannelHandlerContext ctx, String s) throws IOException {
        String[] str = s.split(" ");
        if (str.length == 2) {
            if (str[0].equals("login") && str[1].equals("password"))
                auth = true;
            ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes("OK")));
            System.out.println("correctly login and password");
        }
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
            Path pathDirectory = Path.of(pathCommand[1].substring(0,
                    pathCommand[1].length() - touch[touch.length - 1].length()));
            Files.createDirectory(pathDirectory);
            Files.createFile(Path.of(pathCommand[1]));
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
