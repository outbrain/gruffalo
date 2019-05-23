package com.outbrain.gruffalo.netty;

import com.outbrain.gruffalo.util.Preconditions;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Histogram;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricBatcher extends SimpleChannelInboundHandler<String> {

  private static final Logger log = LoggerFactory.getLogger(MetricBatcher.class);
  private static final AtomicInteger lastBatchSize = new AtomicInteger(0);
  private final int batchBufferCapacity;
  private final Counter connectionCounter;
  private final Counter metricsCounter;
  private final Counter unexpectedErrorCounter;
  private final Counter ioErrorCounter;
  private final Counter idleChannelsClosed;
  private final ChannelGroup activeChannels;
  private final Histogram metricSize;
  private final int maxChannelIdleTime;
  private StringBuilder batch;
  private int currBatchSize;
  private Instant lastRead = Instant.now();

  public MetricBatcher(final MetricFactory metricFactory, final int batchBufferCapacity, final ChannelGroup activeChannels, final int maxChannelIdleTime) {
    Preconditions.checkArgument(maxChannelIdleTime > 0, "maxChannelIdleTime must be greater than 0");
    this.maxChannelIdleTime = maxChannelIdleTime;
    Preconditions.checkNotNull(metricFactory, "metricFactory may not be null");
    this.batchBufferCapacity = batchBufferCapacity;
    this.activeChannels = Preconditions.checkNotNull(activeChannels, "activeChannels must not be null");
    prepareNewBatch();

    final String component = getClass().getSimpleName();
    connectionCounter = metricFactory.createCounter(component, "connections");
    metricsCounter = metricFactory.createCounter(component, "metricsReceived");
    unexpectedErrorCounter = metricFactory.createCounter(component, "unexpectedErrors");
    ioErrorCounter = metricFactory.createCounter(component, "ioErrors");
    idleChannelsClosed = metricFactory.createCounter(component, "idleChannelsClosed");
    metricSize = metricFactory.createHistogram(component, "metricSize", false);
    try {
      metricFactory.registerGauge(component, "batchSize", lastBatchSize::get);
    } catch (IllegalArgumentException e) {
      // ignore metric already exists
    }
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final String msg) {
    lastRead = Instant.now();
    currBatchSize++;
    if (batch.capacity() < batch.length() + msg.length()) {
      sendBatch(ctx);
    }

    batch.append(msg);
    metricsCounter.inc();
    metricSize.update(msg.length());
  }

  private void sendBatch(final ChannelHandlerContext ctx) {
    if (0 < batch.length()) {
      ctx.fireChannelRead(new Batch(batch, currBatchSize));
      prepareNewBatch();
    }
  }

  private void prepareNewBatch() {
    batch = new StringBuilder(batchBufferCapacity);
    lastBatchSize.set(currBatchSize);
    currBatchSize = 0;
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
    if (evt instanceof IdleStateEvent) {
      final IdleStateEvent e = (IdleStateEvent) evt;
      if (e.state() == IdleState.READER_IDLE) {
        sendBatch(ctx);

        final SocketAddress remoteAddress = ctx.channel().remoteAddress();
        if (remoteAddress != null) {
          if (lastRead.plusSeconds(maxChannelIdleTime).isBefore(Instant.now())) {
            log.warn("Closing suspected leaked connection: {}", remoteAddress);
            idleChannelsClosed.inc();
            ctx.close();
            lastRead = Instant.now();
          }
        }
      }
    }
  }

  @Override
  public void channelRegistered(final ChannelHandlerContext ctx) {
    if (ctx.channel().remoteAddress() != null) {
      connectionCounter.inc();
      activeChannels.add(ctx.channel());
    }
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) {
    connectionCounter.dec();
    try {
      sendBatch(ctx);
    } catch (final RuntimeException e) {
      log.warn("failed to send last batch when closing channel " + ctx.channel().remoteAddress());
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    if (cause instanceof IOException) {
      ioErrorCounter.inc();
      log.error("IOException while handling metrics. Remote host =" + ctx.channel().remoteAddress(), cause);
    } else {
      unexpectedErrorCounter.inc();
      log.error("Unexpected exception while handling metrics. Remote host =" + ctx.channel().remoteAddress(), cause);
    }
    ctx.close();
  }
}
