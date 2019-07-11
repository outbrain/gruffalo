package com.outbrain.gruffalo.publish;

import io.netty.buffer.ByteBuf;

public interface MetricsPublisher {

  void publishMetrics(ByteBuf payload);

  void close() throws InterruptedException;
}
