package com.outbrain.gruffalo.netty;


import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.outbrain.gruffalo.util.HostName2MetricName;
import com.outbrain.gruffalo.util.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Time: 8/4/13 12:30 PM
 *
 * @author Eran Harel
 */
public class NettyGraphiteClient implements GraphiteClient {

  private static final Logger log = LoggerFactory.getLogger(NettyGraphiteClient.class);

  private final int inFlightBatchesLowThreshold;
  private final int inFlightBatchesHighThreshold;
  private final Throttler throttler;
  private final AtomicInteger inFlightBatches = new AtomicInteger(0);
  private final Meter errorCounter;
  private final Meter pushBackCounter;
  private final Meter reconnectCounter;
  private final Meter rejectedCounter;
  private final Meter publishedCounter;
  private final Timer publishTime;
  private final String host;
  private GraphiteClientChannelInitializer channelInitializer;
  private volatile ChannelFuture channelFuture;

  public NettyGraphiteClient(final Throttler throttler, final int inFlightBatchesHighThreshold, final MetricRegistry metricRegistry, final String host) {
    Preconditions.checkArgument(0 < inFlightBatchesHighThreshold, "inFlightBatchesHighThreshold must be greater than 0");
    this.inFlightBatchesHighThreshold = inFlightBatchesHighThreshold;
    this.inFlightBatchesLowThreshold = inFlightBatchesHighThreshold / 5;
    Preconditions.checkNotNull(metricRegistry, "metricRegistry must not be null");
    this.throttler = Preconditions.checkNotNull(throttler, "throttler must not be null");
    this.host = host;
    final String graphiteCompatibleHostName = HostName2MetricName.graphiteCompatibleHostPortName(host);
    String component = getClass().getSimpleName();
    errorCounter = metricRegistry.meter(name(component, graphiteCompatibleHostName + "errors"));
    pushBackCounter = metricRegistry.meter(name(component, graphiteCompatibleHostName + "pushBack"));
    reconnectCounter = metricRegistry.meter(name(component, graphiteCompatibleHostName + "reconnect"));
    rejectedCounter = metricRegistry.meter(name(component, graphiteCompatibleHostName + "rejected"));
    publishedCounter = metricRegistry.meter(name(component, graphiteCompatibleHostName + "published"));
    publishTime = metricRegistry.timer(name(component, graphiteCompatibleHostName, "publishTime"));
    metricRegistry.register(name(component, graphiteCompatibleHostName + "inFlightBatches"), (Gauge<Integer>) inFlightBatches::get);
    log.info("Client for [{}] initialized", host);
  }

  public void setChannelInitializer(final GraphiteClientChannelInitializer channelInitializer) {
    this.channelInitializer = channelInitializer;
  }

  @Override
  public void connect() {
    reconnectCounter.mark();
    log.info("Client for [{}] is reconnecting", host);
    channelFuture = channelInitializer.connect();
  }

  @Override
  public boolean publishMetrics(final ByteBuf metrics) {
    if (channelFuture.isDone()) {
      final int numInFlight = inFlightBatches.incrementAndGet();
      if(inFlightBatchesHighThreshold <= numInFlight) {
        onPushBack();
        throttler.pushBackClients();
      }

      channelFuture.channel().writeAndFlush(metrics).addListener(createPublishMetricFutureListener(System.nanoTime()));
      return true;
    } else {
      rejectedCounter.mark();
      return false;
    }
  }

  private ChannelFutureListener createPublishMetricFutureListener(long startTime) {
    return future -> {
      final long elapsed = System.nanoTime() - startTime;
      publishTime.update(elapsed, TimeUnit.NANOSECONDS);

      final int currInFlightBatches = this.inFlightBatches.decrementAndGet();
      if(currInFlightBatches == inFlightBatchesLowThreshold) {
        throttler.restoreClientReads();
      }

      if (future.isSuccess()) {
        publishedCounter.mark();
      } else {
        errorCounter.mark();
        if (log.isDebugEnabled()) {
          log.debug("Failed to write to {}: {}", host, future.cause().toString());
        }
      }
    };
  }

  @Override
  public void onPushBack() {
    pushBackCounter.mark();
  }
}
