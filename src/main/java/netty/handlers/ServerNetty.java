package netty.handlers;

import client.app.javaFX.FileInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ServerNetty extends SimpleChannelInboundHandler<String> {

    private String rootPath = "/";
    private Path directPath = Path.of("server/");
    private String[] pathCommand;
    private List<FileInfo> list;


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

        pathCommand = null;

        if (s.equals("ls")) {
            byte[] sendByte = convertToBytes(getFilesList());
            System.out.println(sendByte.length);
            ByteBuf btf = ctx.alloc().buffer().writeBytes(sendByte);
            ctx.writeAndFlush(btf);
        }

        if (s.equals("path")) {
            ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(convertToBytes(directPath.toString())));

            System.out.println("send");
        }

        if (s.split(" ").length > 1) {
            pathCommand = s.split(" ");
            String[] fragmentPath = pathCommand[1].split("\\\\");

            if (pathCommand[0].equals("cd") && s.split(" ").length > 1) {
                System.out.println(pathCommand[1]);
                if (Files.exists(Path.of(pathCommand[1]))) {
                    rootPath = pathCommand[1];
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

    private boolean validPath (String s) {
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
