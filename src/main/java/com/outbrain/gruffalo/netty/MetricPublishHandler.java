package com.outbrain.gruffalo.netty;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.outbrain.gruffalo.publish.MetricsPublisher;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Objects;

import static com.codahale.metrics.MetricRegistry.name;

@Sharable
public class MetricPublishHandler extends SimpleChannelInboundHandler<Batch> {

  private final MetricsPublisher publisher;
  private final Meter metricsSent;

  public MetricPublishHandler(final MetricsPublisher publisher, final MetricRegistry metricRegistry) {
    Objects.requireNonNull(publisher, "publisher may not be null");
    Objects.requireNonNull(metricRegistry, "metricRegistry may not be null");
    this.publisher = publisher;
    String component = getClass().getSimpleName();
    metricsSent = metricRegistry.meter(name(component, "metricsSent"));
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Batch msg) {
    publisher.publishMetrics(msg.payload.toString());
    metricsSent.mark(msg.batchSize);
  }

}
