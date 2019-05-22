package com.outbrain.gruffalo;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.gruffalo.netty.*;
import com.outbrain.gruffalo.publish.CompoundMetricsPublisher;
import com.outbrain.gruffalo.publish.GraphiteMetricsPublisher;
import com.outbrain.gruffalo.publish.MetricsPublisher;
import com.outbrain.gruffalo.publish.TimedMetricsPublisher;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.codahale3.CodahaleMetricsFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Gruffalo Server main class
 *
 * @author Eran Harel
 */
public class StandaloneGruffaloServer {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneGruffaloServer.class);

  private final MetricFactory metricFactory = new CodahaleMetricsFactory(new MetricRegistry());
  private final DefaultChannelGroup activeServerChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final Throttler throttler = new Throttler(activeServerChannels, metricFactory);
  private final EventLoopGroup eventLoopGroup = createEventLoopGroup();

  public static void main(final String[] args) {
    new StandaloneGruffaloServer().start();
    logger.info("******** Gruffalo started ********");
  }

  private void start() {
    GruffaloProxy proxy = createProxy();
  }

  private GruffaloProxy createProxy() {
    String graphiteRelayHosts = "localhost:2003,localhost:2004"; // TODO introduce proper config
    MetricsPublisher metricsPublisher = createMetricsPublisher(eventLoopGroup, throttler, metricFactory, graphiteRelayHosts);
    TcpServerPipelineFactory tcpServerPipelineFactory = createTcpServerPipelineFactory(metricsPublisher);
    GruffaloProxy proxy = new GruffaloProxy(eventLoopGroup, tcpServerPipelineFactory, 3003/*TODO com.outbrain.gruffalo.tcp.port*/, throttler);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        proxy.shutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));

    return proxy;
  }

  private TcpServerPipelineFactory createTcpServerPipelineFactory(final MetricsPublisher metricsPublisher) {
    final LineBasedFrameDecoderFactory lineBasedFrameDecoderFactory = () -> new LineBasedFrameDecoder(4096/*TODO com.outbrain.gruffalo.netty.maxMetricLength*/, false, true);
    final MetricBatcherFactory metricBatcherFactory = () -> new MetricBatcher(metricFactory, 8192/*TODO com.outbrain.gruffalo.netty.MetricBatcher.batchBufferCapacity*/, activeServerChannels, 120/*TODO com.outbrain.gruffalo.maxChannelIdleTimeSec*/);
    return new TcpServerPipelineFactory(10 /*TODO com.outbrain.gruffalo.netty.readerIdleTimeSeconds*/,
        lineBasedFrameDecoderFactory,
        metricBatcherFactory,
        new MetricPublishHandler(metricsPublisher, metricFactory),
        eventLoopGroup);
  }

  private MetricsPublisher createMetricsPublisher(final EventLoopGroup eventLoopGroup, final Throttler throttler, final MetricFactory metricFactory, final String graphiteRelayHosts) {
    GraphiteClientPool graphiteClient = new GraphiteClientPool(eventLoopGroup, throttler, 1500/*TODO com.outbrain.gruffalo.inFlightBatchesHighThreshold*/, metricFactory, graphiteRelayHosts);
    graphiteClient.connect();
    final MetricsPublisher clutserPublisher = new GraphiteMetricsPublisher(graphiteClient);
    final List<MetricsPublisher> publishers = Collections.singletonList(new TimedMetricsPublisher(clutserPublisher, metricFactory, "mainGraphiteClient"/*TODO add name*/));
    return new CompoundMetricsPublisher(publishers);
  }

  private EventLoopGroup createEventLoopGroup() {
    return new NioEventLoopGroup();
  }

//    <bean id="gruffaloProxy" class="com.outbrain.gruffalo.netty.GruffaloProxy" destroy-method="shutdown">
//    <constructor-arg ref="eventLoopGroup"/>
//    <constructor-arg ref="tcpServerPipelineFactory"/>
//    <constructor-arg value="${com.outbrain.gruffalo.tcp.port}"/>
//    <constructor-arg ref="throttler"/>
//  </bean>
//
//  <bean id="tcpServerPipelineFactory" class="com.outbrain.gruffalo.netty.TcpServerPipelineFactory">
//    <constructor-arg value="${com.outbrain.gruffalo.netty.readerIdleTimeSeconds}"/>
//    <constructor-arg ref="lineFramerFactory"/>
//    <constructor-arg ref="stringDecoder"/>
//    <constructor-arg ref="metricBatcherFactory"/>
//    <constructor-arg ref="publishHandler"/>
//    <constructor-arg ref="publishExecutor"/>
//  </bean>

//  <bean id="publishHandler" class="com.outbrain.gruffalo.netty.MetricPublishHandler">
//    <constructor-arg ref="metricsPublisher"/>
//    <constructor-arg ref="osMetricFactory"/>
//  </bean>
//
//
//  <bean id="nettyQueuesMetricsInitializer" class="com.outbrain.gruffalo.netty.NettyQueuesMetricsInitializer" scope="singleton">
//    <constructor-arg ref="osMetricFactory"/>
//    <constructor-arg ref="eventLoopGroup"/>
//  </bean>
//
//  <!-- ============================================ -->
//  <!--                graphite client               -->
//  <!-- ============================================ -->
//
//  <bean id="graphiteClientTemplate" class="com.outbrain.gruffalo.netty.GraphiteClientPool" init-method="connect" abstract="true">
//    <constructor-arg ref="eventLoopGroup"/>
//    <constructor-arg ref="stringDecoder"/>
//    <constructor-arg ref="stringEncoder"/>
//    <constructor-arg ref="throttler"/>
//    <constructor-arg value="${com.outbrain.gruffalo.inFlightBatchesHighThreshold}"/>
//    <constructor-arg ref="osMetricFactory"/>
//  </bean>
//
//  <bean id="mainGraphiteClient" parent="graphiteClientTemplate">
//    <constructor-arg value="${com.outbrain.metrics.graphite.relay.hosts}"/>
//  </bean>
//
//  <bean id="secondaryGraphiteClient" parent="graphiteClientTemplate" lazy-init="true">
//    <constructor-arg value="${com.outbrain.metrics.graphite.relay.secondary.hosts}"/>
//  </bean>

}
