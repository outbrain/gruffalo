package com.outbrain.gruffalo.publish;

import io.netty.buffer.ByteBuf;

import java.util.LinkedList;
import java.util.List;

/**
 * Time: 10/10/13 2:55 PM
 *
 * @author Eran Harel
 */
public class CompoundMetricsPublisher implements MetricsPublisher {

  private final List<MetricsPublisher> publishers = new LinkedList<>();

  public CompoundMetricsPublisher(final List<MetricsPublisher> publishers) {
    this.publishers.addAll(publishers);
  }

  @Override
  public void publishMetrics(final ByteBuf payload) {
    for (final MetricsPublisher publisher : publishers) {
      publisher.publishMetrics(payload);
    }
  }

  @Override
  public void close() throws InterruptedException {
    for (MetricsPublisher publisher : publishers) {
      publisher.close();
    }
  }
}
