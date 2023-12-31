package com.example.nio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

@Component
@Slf4j
public class Bio implements CommandLineRunner {
    @Override
    public void run(String... args) {
        new Thread(() -> {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(5353);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                while (true) {
                    listen(serverSocket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private static void listen(ServerSocket serverSocket) throws Exception {
        Socket socket = serverSocket.accept();
        //下面我们收取信息
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        handle(socket, in, out);
        //关闭
        out.close();
        in.close();
        socket.close();
    }

    private static void handle(Socket socket, InputStream in, OutputStream out) throws IOException, InterruptedException {
        int sourcePort = socket.getPort();
        byte[] contextBytes = new byte[1024];
        int read = in.read(contextBytes, 0, 1024);
        //读取信息
        String message = new String(contextBytes, 0, read);
        //下面打印信息
        log.info("服务器收到来自于端口: " + sourcePort + "的信息: " + message);
        Thread.sleep(20);
        //下面开始发送信息
        out.write(
                """
                        HTTP/1.1 200 OK
                        Content-Type: text/html;charset=utf-8
                        Content-Length: 11
                        Connection: close

                        hello world
                                """
                        .getBytes());
    }
}