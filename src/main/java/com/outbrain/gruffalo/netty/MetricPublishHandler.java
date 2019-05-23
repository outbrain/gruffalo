package com.outbrain.gruffalo.netty;

import com.outbrain.gruffalo.publish.MetricsPublisher;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

@Sharable
public class MetricPublishHandler extends SimpleChannelInboundHandler<Batch> {

  private final MetricsPublisher publisher;
  private final Timer publishTimer;
  private final Counter metricsCounter;

  public MetricPublishHandler(final MetricsPublisher publisher, final MetricFactory metricFactory) {
    Objects.requireNonNull(publisher, "publisher may not be null");
    Objects.requireNonNull(metricFactory, "metricFactory may not be null");
    this.publisher = publisher;
    String component = getClass().getSimpleName();
    publishTimer = metricFactory.createTimer(component, "publishMetricsBatch");
    metricsCounter = metricFactory.createCounter(component, "metricsSent");
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Batch msg) {
    // TODO this doesn't look right... Timing should be moved to NettyGraphiteClient.publishMetrics, and close timer in the listener
    Timer.Context timerContext = publishTimer.time();
    try {
      publisher.publishMetrics(msg.payload.toString());
      metricsCounter.inc(msg.batchSize);
    } finally {
      timerContext.stop();
    }
  }

}
