import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class NioTelnetServer {

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final String rootPath = "server";
    private String clientPath = "client";

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("localhost", 8189));
        server.configureBlocking(false);
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started!");
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                }
                if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    // TODO: 02.11.2020
    //  ls - список файлов (сделано на уроке),
    //  cd (name) - перейти в папку
    //  touch (name) создать текстовый файл с именем
    //  mkdir (name) создать директорию
    //  rm (name) удалить файл по имени
    //  copy (src, target) скопировать файл из одного пути в другой
    //  cat (name) - вывести в консоль содержимое файла

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int read = channel.read(buffer);
        if (read == -1) {
            channel.close();
            return;
        }
        if (read == 0) {
            return;
        }
        buffer.flip();
        byte[] buf = new byte[read];
        int pos = 0;
        while (buffer.hasRemaining()) {
            buf[pos++] = buffer.get();
        }
        buffer.clear();
        String command = new String(buf, StandardCharsets.UTF_8)
                .replace("\n", "")
                .replace("\r", "");
        String[] cmd = command.split(" ");
        System.out.println(command);
        switch (cmd[0]) {
            case "--help" -> channel.write(ByteBuffer.wrap("input ls for show file list\n".getBytes()));
            case "ls" -> {
                channel.write(ByteBuffer.wrap(getFilesList().getBytes()));
                channel.write(ByteBuffer.wrap("\n".getBytes()));
            }
            case "cd" -> {
                Path cd = Paths.get(clientPath, cmd[1]);
                channel.write(ByteBuffer.wrap(cd.toAbsolutePath().toString().getBytes()));
                channel.write(ByteBuffer.wrap("\n".getBytes()));
                clientPath = cd.toAbsolutePath().toString();
            }
            case "touch" -> {
                Path touch = Path.of(clientPath, cmd[1]);
                if (!Files.exists(touch)) {
                    Files.createFile(touch);
                }
            }
            case "mkdir" -> {
                Path dir = Path.of(clientPath, cmd[1]);
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
            }
            case "rm" -> {
                Path rm = Paths.get(clientPath, cmd[1]);
                if (Files.isDirectory(rm)) {
                    Files.walkFileTree(rm, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            System.out.println("delete file: " + file.toString());
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            System.out.println("delete dir: " + dir.toString());
                            return FileVisitResult.CONTINUE;
                        }
                    });

                } else {
                    Files.delete(rm);
                    System.out.println("delete file: " + rm.toAbsolutePath().toString());
                }
            }
            case "copy" -> {
                Path pathSource = Paths.get(cmd[1]);
                Path pathDestination = Paths.get(cmd[2]);
                Files.copy(pathSource, pathDestination, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Source file copied successfully");
            }
            case "cat" -> {
                Path cat = Paths.get(clientPath, cmd[1]);
                channel.write(ByteBuffer.wrap(Files.readAllLines(cat).toString().getBytes()));
                channel.write(ByteBuffer.wrap("\n".getBytes()));
            }
            case "-back" -> {
                Path path = Paths.get(clientPath);
                channel.write(ByteBuffer.wrap(path.getParent().toAbsolutePath().toString().getBytes()));
                channel.write(ByteBuffer.wrap("\n".getBytes()));
                clientPath = path.getParent().toAbsolutePath().toString();
            }
        }
    }

    private String getFilesList() {
        return String.join("\n", new File(clientPath).list());
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "LOL");
        channel.write(ByteBuffer.wrap("Enter: ".getBytes()));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
