package net.techtrends.network.pipeline.in;

import net.techtrends.listeners.response.Callback;
import net.techtrends.network.AggregateCallback;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

public class PipelineInBuilder {

    private AsynchronousSocketChannel client;
    private boolean allocateDirect = false;
    private int bufferSize;
    private AggregateCallback aggregateCallback;

    public PipelineInBuilder configureAggregateCallback(List<Callback> callbacks) {
        this.aggregateCallback = new AggregateCallback(callbacks);
        return this;
    }
    public PipelineInBuilder setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public PipelineInBuilder client(AsynchronousSocketChannel client) {
        this.client = client;
        return this;
    }

    public PipelineInBuilder allocateDirect(boolean allocateDirect) {
        this.allocateDirect = allocateDirect;
        return this;
    }

    public PipelineIn build() {
        return new PipelineIn(client, allocateDirect, bufferSize, aggregateCallback);
    }
}
