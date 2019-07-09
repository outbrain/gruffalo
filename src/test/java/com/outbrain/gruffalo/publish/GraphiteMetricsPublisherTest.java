package com.outbrain.gruffalo.publish;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.gruffalo.netty.GraphiteChannelInboundHandler;
import com.outbrain.gruffalo.netty.GraphiteClientChannelInitializer;
import com.outbrain.gruffalo.netty.NettyGraphiteClient;
import com.outbrain.gruffalo.netty.Throttler;
import io.netty.buffer.Unpooled;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

/**
 * Time: 7/29/13 11:27 AM
 *
 * @author Eran Harel
 */
public class GraphiteMetricsPublisherTest {

  public static void main(String[] args) throws InterruptedException {

    final MetricRegistry metricRegistry = new MetricRegistry();

    NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    final Throttler throttler = new Throttler(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE), metricRegistry);
    NettyGraphiteClient client = new NettyGraphiteClient(throttler, 1000, metricRegistry, "localhost:666");
    String host = "localhost";
    int port = 3003;
    GraphiteClientChannelInitializer channelInitializer = new GraphiteClientChannelInitializer(host, port, eventLoopGroup, new GraphiteChannelInboundHandler(client, host + ":" + port, throttler));
    client.setChannelInitializer(channelInitializer);
    client.connect();

//    Thread.sleep(20000);
    System.out.println("Begin bombardment...");
    StopWatch time = new StopWatch();
    time.start();
    for (int i = 0; i < 10000000; i++) {
      byte[] metricsBytes = (i + " - 0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n").getBytes(CharsetUtil.UTF_8);
      client.publishMetrics(Unpooled.wrappedBuffer(metricsBytes));
      if(i % 10000 == 0) {
        Thread.sleep(100);
      }
      if (i % 100000 == 0) {
        System.out.println(i);
        Thread.sleep(300);
      }
    }
    time.stop();

    System.out.println("sent all data: " + time +"; shutting down...");

    Thread.sleep(1000000);
    eventLoopGroup.shutdownGracefully(10, 20, TimeUnit.SECONDS);
  }

}
