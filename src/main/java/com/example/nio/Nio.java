package com.example.nio;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

@Component
@Slf4j
public class Nio implements CommandLineRunner {
    private static final ExecutorService executorService = Executors.newFixedThreadPool(100);
    private final BlockingDeque<Message> message = new LinkedBlockingDeque<>();

    @Override
    public void run(String... args) throws Exception {
        new Thread(Nio::listen).start();
    }

    @Data
    static class Message {
        private SocketChannel socketChannel;
        private String message;
    }

    public static void listen() {
        try (Selector selector = Selector.open()) {
            try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                serverSocketChannel.socket().bind(new InetSocketAddress(5355));
                while (true) {
                    selector.select();
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    for (SelectionKey key : selectionKeys) {
                        if (key.isAcceptable()) {
                            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                            SocketChannel newSocketChannel = channel.accept();
                            registerSocketChannel(newSocketChannel, selector);
                        } else if (key.isValid() && key.isReadable()) {
                            handelRead(key);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("listen error", e);
            }
        } catch (Exception e) {
            log.error("listen error", e);
        }
    }

    private static void registerSocketChannel(SocketChannel socketChannel, Selector selector) throws Exception {
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
    }

    private static void handelRead(SelectionKey readyKey) throws Exception {
        SocketChannel clientSocketChannel = (SocketChannel) readyKey.channel();
        int resourcePort = clientSocketChannel.socket().getPort();

        ByteBuffer contextBytes = (ByteBuffer) readyKey.attachment();
        clientSocketChannel.read(contextBytes);

        contextBytes.flip();
        byte[] messageBytes = new byte[contextBytes.limit()];
        contextBytes.get(messageBytes);
        String message = new String(messageBytes);
        contextBytes.flip();

        //如果发现本次接收的信息中有over关键字，说明信息接收完了
        if (message.endsWith("\r\n\r\n")) {
            contextBytes.clear();
            log.info("服务器收到来自于端口: " + resourcePort + "的信息: " + message);
            Future<?> writeError = executorService.submit(() -> {
                try {
                    Thread.sleep(20);
                    byte[] bytes = """
                            HTTP/1.1 200 OK
                            Content-Type: text/html;charset=utf-8
                            Content-Length: 11
                            Connection: keep-alive

                            hello world
                                    """.getBytes();
                    clientSocketChannel.write(ByteBuffer.wrap(bytes));
                } catch (Exception e) {
                    log.error("write error", e);
                }
            });
        }
    }
}