package netty.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerNetty extends SimpleChannelInboundHandler<String> {

    private String rootPath = "/";
    private String[] pathCommand;


    /*public static final ConcurrentLinkedDeque<ChannelHandlerContext> channels =
            new ConcurrentLinkedDeque<>();*/


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        System.out.println("Message from client: " + s);

        pathCommand = null;

        if (s.equals("ls")) {
            ctx.writeAndFlush(getFilesList());
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

        /*s = s.replaceAll("fuck", "****");
        String finalS = s;
        channels.forEach(c -> c.writeAndFlush(finalS));*/
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

    private String getFilesList() {
        return String.join(" ", new File(rootPath).list());
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client disconnected");
    }
}
