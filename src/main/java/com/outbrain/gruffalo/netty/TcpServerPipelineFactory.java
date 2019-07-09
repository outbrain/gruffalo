package com.outbrain.gruffalo.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Objects;

public class TcpServerPipelineFactory extends ChannelInitializer<Channel> {

  private final int readerIdleTimeSeconds;
  private final LineBasedFrameDecoderFactory framerFactory;
  private final MetricBatcherFactory metricBatcherFactory;
  private final MetricPublishHandler publishHandler;

  public TcpServerPipelineFactory(final int readerIdleTimeSeconds,
                                  final LineBasedFrameDecoderFactory framerFactory,
                                  final MetricBatcherFactory metricBatcherFactory,
                                  final MetricPublishHandler publishHandler) {
    Objects.requireNonNull(framerFactory, "framerFactory,  may not be null");
    Objects.requireNonNull(metricBatcherFactory, "metricBatcherFactory may not be null");
    Objects.requireNonNull(publishHandler, "publishHandler may not be null");

    this.readerIdleTimeSeconds = readerIdleTimeSeconds;
    this.framerFactory = framerFactory;
    this.metricBatcherFactory = metricBatcherFactory;
    this.publishHandler = publishHandler;
  }

  @Override
  protected void initChannel(final Channel channel) {
    final ChannelPipeline pipeline = channel.pipeline();
    pipeline.addLast("idleStateHandler", new IdleStateHandler(readerIdleTimeSeconds, 0, 0));
    pipeline.addLast("framer", framerFactory.getLineFramer());
    pipeline.addLast("batchHandler", metricBatcherFactory.getMetricBatcher());
    pipeline.addLast("publishHandler", publishHandler);
  }

}
