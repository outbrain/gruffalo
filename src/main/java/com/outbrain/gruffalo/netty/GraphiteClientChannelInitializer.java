package com.outbrain.gruffalo.netty;


import com.outbrain.gruffalo.util.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
* Time: 8/4/13 10:06 AM
*
* @author Eran Harel
*/
public class GraphiteClientChannelInitializer extends ChannelInitializer<Channel> {

  private final String graphiteHost;
  private final int graphitePort;
  private final EventLoopGroup eventLoopGroup;
  private final ChannelHandler graphiteHandler;

  public GraphiteClientChannelInitializer(final String graphiteHost,
                                          final int graphitePort,
                                          final EventLoopGroup eventLoopGroup,
                                          final ChannelHandler graphiteHandler) {
    this.graphiteHost = graphiteHost;
    this.graphitePort = graphitePort;
    this.graphiteHandler = Preconditions.checkNotNull(graphiteHandler, "graphiteHandler must not be null");
    this.eventLoopGroup = Preconditions.checkNotNull(eventLoopGroup, "eventLoopGroup must not be null");
  }

  ChannelFuture connect() {
    return configureBootstrap().connect();
  }

  private Bootstrap configureBootstrap() {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.remoteAddress(graphiteHost, graphitePort);
    bootstrap.group(eventLoopGroup);
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.handler(this);
    bootstrap.option(ChannelOption.SO_LINGER, 0);
    bootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);

    return bootstrap;
  }

  @Override
  protected void initChannel(Channel channel) {
    ChannelPipeline pipeline = channel.pipeline();

    pipeline.addLast(new IdleStateHandler(0, 10, 0));
    pipeline.addLast("handler", graphiteHandler);
  }
}
