package com.outbrain.gruffalo.netty;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.outbrain.gruffalo.publish.MetricsPublisher;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

import static com.codahale.metrics.MetricRegistry.name;

@Sharable
public class MetricPublishHandler extends SimpleChannelInboundHandler<Batch> {

  private final MetricsPublisher publisher;
  private final Timer publishTimer;
  private final Meter metricsSent;

  public MetricPublishHandler(final MetricsPublisher publisher, final MetricRegistry metricRegistry) {
    Objects.requireNonNull(publisher, "publisher may not be null");
    Objects.requireNonNull(metricRegistry, "metricRegistry may not be null");
    this.publisher = publisher;
    String component = getClass().getSimpleName();
    publishTimer = metricRegistry.timer(name(component, "publishMetricsBatch"));
    metricsSent = metricRegistry.meter(name(component, "metricsSent"));
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Batch msg) {
    // TODO this doesn't look right... Timing should be moved to NettyGraphiteClient.publishMetrics, and close timer in the listener
    Timer.Context timerContext = publishTimer.time();
    try {
      publisher.publishMetrics(msg.payload.toString());
      metricsSent.mark(msg.batchSize);
    } finally {
      timerContext.stop();
    }
  }

}
