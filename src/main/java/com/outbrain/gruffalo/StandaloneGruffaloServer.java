package com.outbrain.gruffalo;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.gruffalo.config.Config;
import com.outbrain.gruffalo.netty.*;
import com.outbrain.gruffalo.publish.CompoundMetricsPublisher;
import com.outbrain.gruffalo.publish.GraphiteMetricsPublisher;
import com.outbrain.gruffalo.publish.MetricsPublisher;
import com.outbrain.gruffalo.publish.TimedMetricsPublisher;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Gruffalo Server main class
 *
 * @author Eran Harel
 */
public class StandaloneGruffaloServer {
  private static final Logger logger = LoggerFactory.getLogger(StandaloneGruffaloServer.class);

  private final Config config;
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private final DefaultChannelGroup activeServerChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
  private final Throttler throttler = new Throttler(activeServerChannels, new MetricRegistry());
  private final EventLoopGroup eventLoopGroup = createEventLoopGroup();

  private StandaloneGruffaloServer(final Config config) {
    this.config = config;
    MetricsPublisher metricsPublisher = createMetricsPublisher(eventLoopGroup, throttler, metricRegistry, config.graphiteClusters);
    TcpServerPipelineFactory tcpServerPipelineFactory = createTcpServerPipelineFactory(metricsPublisher);

    GruffaloProxy proxy = createProxy(config.port, tcpServerPipelineFactory);
  }

  public static void main(final String[] args) {
    Config config = Config.parseCommand(StandaloneGruffaloServer.class.getName(), args);
    if (config == null) {
      return;
    }

    new StandaloneGruffaloServer(config);
    logger.info("******** Gruffalo started ********");
  }

  private GruffaloProxy createProxy(final int tcpPort, final TcpServerPipelineFactory tcpServerPipelineFactory) {
    GruffaloProxy proxy = new GruffaloProxy(eventLoopGroup, tcpServerPipelineFactory, tcpPort, throttler);
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
    final LineBasedFrameDecoderFactory lineBasedFrameDecoderFactory = () -> new LineBasedFrameDecoder(config.maxMetricLength, false, true);
    final MetricBatcherFactory metricBatcherFactory = () -> new MetricBatcher(metricRegistry, config.maxBatchSize, activeServerChannels, config.reconnectOnIdleTime);
    return new TcpServerPipelineFactory(config.flushOnIdleTime,
        lineBasedFrameDecoderFactory,
        metricBatcherFactory,
        new MetricPublishHandler(metricsPublisher, metricRegistry));
  }

  private MetricsPublisher createMetricsPublisher(final EventLoopGroup eventLoopGroup, final Throttler throttler, final MetricRegistry metricRegistry, final String[] clusters) {
    final List<MetricsPublisher> publishers = new ArrayList<>(clusters.length);
    for (int i = 0; i < clusters.length; i++) {
      GraphiteClientPool graphiteClient = new GraphiteClientPool(eventLoopGroup, throttler, config.maxInflightBatches, metricRegistry, clusters[i]);
      graphiteClient.connect();

      final MetricsPublisher clutserPublisher = new GraphiteMetricsPublisher(graphiteClient);
      publishers.add(new TimedMetricsPublisher(clutserPublisher, metricRegistry, "graphiteCluster-" + i));
    }

    return new CompoundMetricsPublisher(publishers);
  }

  private EventLoopGroup createEventLoopGroup() {
    return new NioEventLoopGroup();
  }

}
