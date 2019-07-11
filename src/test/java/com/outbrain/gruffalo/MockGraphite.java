package com.outbrain.gruffalo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;

/**
 * A simple graphite server that counts occurrences of "metric names".
 * Each received metric is treated as a key for the count.
 *
 * @author Eran Harel
 */
public class MockGraphite implements Closeable {

  private static final Logger log = LoggerFactory.getLogger(MockGraphite.class);

  private final Map<String, Integer> metric2count = new ConcurrentHashMap<>();
  private final EventLoopGroup eventLoopGroup;

  {
    final ThreadFactory threadFactory = new BasicThreadFactory.Builder().namingPattern("MockGraphite-eventloop").build();
    eventLoopGroup = new NioEventLoopGroup(1, threadFactory);
  }

  private final ChannelFuture serverChannel;

  public MockGraphite() throws InterruptedException {
    MetricsHandler metricsHandler = new MetricsHandler();
    final ServerBootstrap b = new ServerBootstrap();
    b.group(eventLoopGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) {
            final ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new LineBasedFrameDecoder(1024, true, false));
            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
            pipeline.addLast(metricsHandler);
          }
        });

    serverChannel = b.bind(0).sync();
    log.info("Server started on port " + getPort());
  }

  int getPort() {
    return ((InetSocketAddress) serverChannel.channel().localAddress()).getPort();
  }

  int getCount(final String metric) {
    return metric2count.getOrDefault(metric, 0);
  }

  @Override
  public void close() {
    log.info("Shutting down mock graphite server");
    try {
      serverChannel.channel().close();
      serverChannel.channel().closeFuture().sync();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      eventLoopGroup.shutdownGracefully();
    }
  }

  @ChannelHandler.Sharable
  private class MetricsHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
      log.debug("Got metric: {}", msg);
      metric2count.compute(msg, (k, count) -> count == null ? 1 : count + 1);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
      log.error("Unexpected exception while handling inbound stream: {}", cause.getMessage());
      ctx.close();
    }

  }

  public static void main(String[] args) throws InterruptedException, IOException {
    MockGraphite mockGraphite = new MockGraphite();
    Thread.sleep(30000);

    System.out.println(mockGraphite.metric2count);
    mockGraphite.close();
  }
}
