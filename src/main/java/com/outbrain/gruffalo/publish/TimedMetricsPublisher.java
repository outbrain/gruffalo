package com.outbrain.gruffalo.publish;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

/**
 * A thin layer on top of a {@link com.outbrain.gruffalo.publish.MetricsPublisher} adding timing aspect
 *
 * @author Eran Harel
 */
public class TimedMetricsPublisher implements MetricsPublisher {

  private final MetricsPublisher timedDelegate;
  private final Timer timer;

  public TimedMetricsPublisher(final MetricsPublisher timedDelegate, final MetricRegistry metricRegistry, final String publisherName) {
    this.timedDelegate = timedDelegate;
    timer = metricRegistry.timer(MetricRegistry.name(timedDelegate.getClass().getSimpleName(), publisherName));
  }

  @Override
  public void publishMetrics(final String payload) {
    // TODO this doesn't look right... Timing should be moved to NettyGraphiteClient.publishMetrics, and close timer in the listener
    final Timer.Context timerContext = timer.time();
    try {
      timedDelegate.publishMetrics(payload);
    } finally {
      timerContext.stop();
    }
  }
}
