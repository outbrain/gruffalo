package com.outbrain.gruffalo;

import com.google.common.eventbus.Subscribe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReceivedMetricsListener {
  private final Map<String, Integer> metric2count = new ConcurrentHashMap<>();
  private final CountDownLatch wg;

  ReceivedMetricsListener(final int expectedCount) {
    this.wg = new CountDownLatch(expectedCount);
  }

  @Subscribe
  public void metricReceived(final String metric) {
    metric2count.compute(metric, (k, count) -> count == null ? 1 : count + 1);
    wg.countDown();
  }

  int getCount(final String metric) {
    return metric2count.getOrDefault(metric, 0);
  }

  void await(final long timeout, final TimeUnit unit) throws InterruptedException {
    wg.await(timeout, unit);
  }
}
