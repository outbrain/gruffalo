package com.outbrain.gruffalo.netty;

import com.codahale.metrics.*;
import com.outbrain.gruffalo.util.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;

public class MetricBatcher extends SimpleChannelInboundHandler<ByteBuf> {

  private static final Logger log = LoggerFactory.getLogger(MetricBatcher.class);
  private static final AtomicInteger lastBatchSize = new AtomicInteger(0);
  private static final ByteBuf[] EMPTY_BUFFS_ARR = new ByteBuf[0];
  private final int batchBufferCapacity;
  private final Counter connections;
  private final Meter metricsReceived;
  private final Meter unexpectedErrors;
  private final Meter ioErrors;
  private final Meter idleChannelsClosed;
  private final Histogram metricSize;
  private final ChannelGroup activeChannels;
  private final int maxChannelIdleTime;
  private final List<ByteBuf> batch = new ArrayList<>(128);
  private int currentBatchLength;
  private Instant lastRead = Instant.now();

  public MetricBatcher(final MetricRegistry metricRegistry, final int batchBufferCapacity, final ChannelGroup activeChannels, final int maxChannelIdleTime) {
    super(false);
    Preconditions.checkArgument(maxChannelIdleTime > 0, "maxChannelIdleTime must be greater than 0");
    this.maxChannelIdleTime = maxChannelIdleTime;
    Preconditions.checkNotNull(metricRegistry, "metricRegistry may not be null");
    this.batchBufferCapacity = batchBufferCapacity;
    this.activeChannels = Preconditions.checkNotNull(activeChannels, "activeChannels must not be null");
    prepareNewBatch();

    final String component = getClass().getSimpleName();
    connections = metricRegistry.counter(name(component, "connections"));
    metricsReceived = metricRegistry.meter(name(component, "metricsReceived"));
    unexpectedErrors = metricRegistry.meter(name(component, "unexpectedErrors"));
    ioErrors = metricRegistry.meter(name(component, "ioErrors"));
    idleChannelsClosed = metricRegistry.meter(name(component, "idleChannelsClosed"));
    metricSize = metricRegistry.histogram(name(component, "metricSize"));
    try {
      metricRegistry.register(name(component, "batchSize"), (Gauge<Integer>)lastBatchSize::get);
    } catch (IllegalArgumentException e) {
      // ignore metric already exists
    }
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
    lastRead = Instant.now();
    int metricLength = msg.capacity();
    if (batchBufferCapacity < currentBatchLength + metricLength) {
      sendBatch(ctx);
    }

    currentBatchLength += metricLength;
    batch.add(msg);
    metricsReceived.mark();
    metricSize.update(metricLength);
  }

  private void sendBatch(final ChannelHandlerContext ctx) {
    if (0 < batch.size()) {
      final ByteBuf batchBuf = Unpooled.wrappedBuffer(batch.toArray(EMPTY_BUFFS_ARR));
      ctx.fireChannelRead(new Batch(batchBuf, batch.size()));
      prepareNewBatch();
    }
  }

  private void prepareNewBatch() {
    lastBatchSize.set(batch.size());
    batch.clear();
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
            idleChannelsClosed.mark();
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
      connections.inc();
      activeChannels.add(ctx.channel());
    }
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) {
    connections.dec();
    try {
      sendBatch(ctx);
    } catch (final RuntimeException e) {
      log.warn("failed to send last batch when closing channel " + ctx.channel().remoteAddress());
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
    if (cause instanceof IOException) {
      ioErrors.mark();
      log.error("IOException while handling metrics. Remote host =" + ctx.channel().remoteAddress(), cause);
    } else {
      unexpectedErrors.mark();
      log.error("Unexpected exception while handling metrics. Remote host =" + ctx.channel().remoteAddress(), cause);
    }
    ctx.close();
  }
}
