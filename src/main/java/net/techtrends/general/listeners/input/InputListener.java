package net.techtrends.general.listeners.input;

import net.techtrends.client.AsyncSocket;
import net.techtrends.general.listeners.ResponseCallback;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The InputListener class is responsible for handling input events and receiving
 * data through an AsynchronousSocketChannel. It implements the CompletionHandler
 * interface to provide a way to process and receive data from the channel.
 * This class maintains a ByteBuffer for incoming data and provides a method to read
 * the data when it is available.
 */
public class InputListener implements CompletionHandler<Integer, ByteBuffer> {
    private final AsynchronousSocketChannel socketChannel;
    private ByteBuffer inputBuffer;
    private final ResponseCallback responseCallback;
    private final int maxBufferSize;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public InputListener(AsynchronousSocketChannel socketChannel, boolean allocateDirect, int initialBufferSize, int maxBufferSize, ResponseCallback responseCallback) {
        this.socketChannel = socketChannel;
        this.responseCallback = responseCallback;
        this.maxBufferSize = maxBufferSize;
        if (allocateDirect) {
            this.inputBuffer = ByteBuffer.allocateDirect(initialBufferSize);
        } else {
            this.inputBuffer = ByteBuffer.allocate(initialBufferSize);
        }
    }

    public void start() {
        socketChannel.read(inputBuffer, inputBuffer, this);
    }

    @Override
    public void completed(Integer bytesRead, ByteBuffer buffer) {
        if (bytesRead < 0) {
            AsyncSocket.closeSocketChannel(socketChannel);
            return;
        }
        if (bytesRead > 0) {

            if (buffer.remaining() < maxBufferSize && buffer.capacity() < maxBufferSize) {
                double bufferUsagePercentage = (double) buffer.position() / buffer.capacity();
                System.out.println(bufferUsagePercentage);
                if (bufferUsagePercentage >= 0.75) {
                    ByteBuffer newBuffer = buffer.isDirect() ? ByteBuffer.allocateDirect(Math.min(buffer.capacity() * 2, maxBufferSize)) : ByteBuffer.allocate(Math.min(buffer.capacity() * 2, maxBufferSize));
                    buffer.flip();
                    newBuffer.put(buffer);
                    inputBuffer = newBuffer;
                }
            } else {
                AsyncSocket.closeSocketChannel(socketChannel);
                System.out.println("Packet rejected: buffer size exceeds maxBufferSize!");
                return;
            }


            buffer.flip();
            byte marker = buffer.get();
            if (marker >= 0x01 && marker <= 0x06) {
                switch (marker) {
                    case 0x01 -> handleString(buffer, responseCallback);
                    case 0x02 -> handleInt(buffer, responseCallback);
                    case 0x03 -> handleFloat(buffer, responseCallback);
                    case 0x04 -> handleDouble(buffer, responseCallback);
                    case 0x05 -> handleChar(buffer, responseCallback);
                    case 0x06 -> handleByteArray(buffer, responseCallback);
                }
            } else {
                AsyncSocket.closeSocketChannel(socketChannel);
                System.out.println("Invalid marker received!");
            }
            buffer.clear();
            inputBuffer.clear();
        }
        socketChannel.read(inputBuffer, inputBuffer, this);
    }

    private void handleString(ByteBuffer buffer, ResponseCallback callback) {
        String data = StandardCharsets.UTF_8.decode(buffer).toString();

        String sanitizedData = sanitizeString(data);

        CompletableFuture.runAsync(() -> callback.complete(sanitizedData), executorService);
    }

    private String sanitizeString(String data) {
        String allowedCharactersRegex = "[^A-Za-z0-9]+";
        return data.replaceAll(allowedCharactersRegex, "");
    }


    private void handleInt(ByteBuffer buffer, ResponseCallback callback) {
        int data = buffer.getInt();
        CompletableFuture.runAsync(() -> callback.complete(data), executorService);
    }

    private void handleFloat(ByteBuffer buffer, ResponseCallback callback) {
        float data = buffer.getFloat();
        CompletableFuture.runAsync(() -> callback.complete(data), executorService);
    }

    private void handleDouble(ByteBuffer buffer, ResponseCallback callback) {
        double data = buffer.getDouble();
        CompletableFuture.runAsync(() -> callback.complete(data), executorService);
    }

    private void handleChar(ByteBuffer buffer, ResponseCallback callback) {
        char data = buffer.getChar();
        CompletableFuture.runAsync(() -> callback.complete(data), executorService);
    }

    private void handleByteArray(ByteBuffer buffer, ResponseCallback callback) {
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        CompletableFuture.runAsync(() -> callback.complete(data), executorService);
    }

    @Override
    public void failed(Throwable exc, ByteBuffer buffer) {
        AsyncSocket.closeSocketChannel(socketChannel);
        System.out.println("Client disconnect!");
    }


    public void close() {
        AsyncSocket.closeSocketChannel(socketChannel);
        executorService.shutdown();
    }
}
