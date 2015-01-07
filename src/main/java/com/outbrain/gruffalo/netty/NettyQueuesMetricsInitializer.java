package com.outbrain.gruffalo.netty;

import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.SingleThreadEventExecutor;

/**
 * Registers metrics on Netty's even loop queues
 * @author Eran Harel
 */
class NettyQueuesMetricsInitializer {

  public NettyQueuesMetricsInitializer(final MetricFactory metricFactory, final EventLoopGroup elg) {
    if (metricFactory == null || elg == null) {
      return;
    }

    int index = 0;
    for (final EventExecutor eventExecutor : elg) {
      if (eventExecutor instanceof SingleThreadEventExecutor) {
        final SingleThreadEventExecutor singleExecutor = (SingleThreadEventExecutor) eventExecutor;
        metricFactory.registerGauge("GruffaloEventLoopGroup", "EventLoop-" + index, new Gauge<Integer>() {
          @Override
          public Integer getValue() {
            return singleExecutor.pendingTasks();
          }
        });

        index++;
      }
    }
  }

}