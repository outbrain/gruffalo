package com.outbrain.gruffalo.publish;

import io.netty.buffer.ByteBuf;

public interface MetricsPublisher {

  public void publishMetrics(ByteBuf payload);
}
