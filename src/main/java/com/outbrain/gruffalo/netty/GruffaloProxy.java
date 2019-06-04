package com.outbrain.gruffalo.netty;


import com.outbrain.gruffalo.util.Preconditions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GruffaloProxy {

  private static final Logger log = LoggerFactory.getLogger(GruffaloProxy.class);
  private final ChannelFuture tcpChannelFuture;
  private final EventLoopGroup eventLoopGroup;
  private final Throttler throttler;

  public GruffaloProxy(final EventLoopGroup eventLoopGroup,
                       final TcpServerPipelineFactory tcpServerPipelineFactory,
                       final int tcpPort,
                       final Throttler throttler) {
    this.throttler = Preconditions.checkNotNull(throttler, "throttler must not be null");
    this.eventLoopGroup = Preconditions.checkNotNull(eventLoopGroup, "eventLoopGroup must not be null");
    tcpChannelFuture = createTcpBootstrap(tcpServerPipelineFactory, tcpPort);
    log.info("Initialization completed");
  }

  public void withShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        shutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));
  }

  private ChannelFuture createTcpBootstrap(final TcpServerPipelineFactory tcpServerPipelineFactory, final int tcpPort) {
    log.info("Initializing TCP...");
    ServerBootstrap tcpBootstrap = new ServerBootstrap();
    tcpBootstrap.group(eventLoopGroup);
    tcpBootstrap.channel(NioServerSocketChannel.class);
    tcpBootstrap.childHandler(tcpServerPipelineFactory);
    tcpBootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);

    final ChannelFuture channelFuture = tcpBootstrap.bind(tcpPort).addListener((ChannelFutureListener) future -> throttler.setServerChannel(future.channel()));
    log.info("Binding to TCP port {}", tcpPort);
    return channelFuture;
  }

  public void shutdown() throws InterruptedException {
    log.info("Shutting down inbound channels...");
    tcpChannelFuture.sync().channel().closeFuture().await(200);

    log.info("Shutting down server...");
    eventLoopGroup.shutdownGracefully();
  }
}
