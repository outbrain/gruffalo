package com.outbrain.gruffalo.publish;


import com.outbrain.gruffalo.netty.GraphiteClient;
import com.outbrain.gruffalo.util.Preconditions;
import io.netty.buffer.ByteBuf;

/**
 * Time: 7/28/13 3:16 PM
 *
 * @author Eran Harel
 */
public class GraphiteMetricsPublisher implements MetricsPublisher {

  private final GraphiteClient graphiteClient;

  public GraphiteMetricsPublisher(final GraphiteClient graphiteClient) {
    this.graphiteClient = Preconditions.checkNotNull(graphiteClient, "graphiteClient must not be null");
  }

  @Override
  public void publishMetrics(final ByteBuf metrics) {
    graphiteClient.publishMetrics(metrics);
  }

}
