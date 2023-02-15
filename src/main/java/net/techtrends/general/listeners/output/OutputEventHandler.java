package net.techtrends.general.listeners.output;

import net.techtrends.general.listeners.ResponseCallback;

import java.nio.channels.AsynchronousSocketChannel;


/*
 *    This is an interface representing an event handler for output events from an AsynchronousSocketChannel.
 *    It provides a single method handle() that takes an AsynchronousSocketChannel, a data, a ResponseCallback as parameters.
 *    The method is responsible for writing data from the socket channel and invoking the appropriate ResponseCallback
 *    method to pass the data back to the calling code.
 *    @param <T> the type of data to be read from the socket channel and passed back to the calling code
 */

public interface OutputEventHandler {

    /**
     * Handles output operations on the socket channel, sending the given value to the
     * client and invoking the provided callback when the operation is complete.
     *
     * @param socketChannel the socket channel to write data to
     * @param value the value to send to the client
     */

    void handle(AsynchronousSocketChannel socketChannel, Object value);

}
