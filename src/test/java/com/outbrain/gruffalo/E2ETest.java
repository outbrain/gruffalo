package com.outbrain.gruffalo;

import com.google.common.eventbus.EventBus;
import com.outbrain.gruffalo.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * End To End test for the entire server stack.
 * @author Eran Harel
 */
class E2ETest {

  private static int serverPort = 3003;

  private MockGraphite mockGraphite;
  private StandaloneGruffaloServer gruffaloServer;
  private EventBus eventBus;

  @BeforeEach
  void setup() throws Exception {
    serverPort++;
    eventBus = new EventBus();
    mockGraphite = new MockGraphite(eventBus);
    Config config = Config.parseCommand(
        "test.gruffalo",
        new String[]{"-p", String.valueOf(serverPort), "-c", "localhost:" + mockGraphite.getPort(), "-maxBatchSize", "10"});
    gruffaloServer = new StandaloneGruffaloServer(config);
  }

  @AfterEach
  void teardown() throws Exception {
    eventBus = null;
    gruffaloServer.shutdown();
    mockGraphite.close();
  }

  @Test
  void testGruffaloProxy_singleMetric() throws Exception {
    final ReceivedMetricsListener metricsListener = createListener(2);
    GraphiteClient client = new GraphiteClient(serverPort);

    String metric = "0-aaaaaaa";
    client.send(metric);
    client.send(metric);
    client.close();

    metricsListener.await(1, TimeUnit.SECONDS);
    Assertions.assertEquals(2, metricsListener.getCount("0-aaaaaaa"));
  }

  @Test
  void testGruffaloProxy_concurrentThreads() throws Exception {
    final int numThreads = 4;
    final int numMetrics = 500;
    final String metricFormat = "metric-%d";

    ReceivedMetricsListener metricsListener = createListener(numMetrics * numThreads);

    for (int i = 0; i < numThreads; i++) {
      new Thread(() -> {
        try {
          GraphiteClient client = new GraphiteClient(serverPort);
          for (int j = 0; j < numMetrics; j++) {
            client.send(String.format(metricFormat, j));
          }

          client.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();
    }

    metricsListener.await(1, TimeUnit.SECONDS);

    for (int i = 0; i < numThreads; i++) {
      Assertions.assertEquals(numThreads, metricsListener.getCount(String.format(metricFormat, i)));
    }
  }

  private ReceivedMetricsListener createListener(final int expectedCount) {
    final ReceivedMetricsListener metricsListener = new ReceivedMetricsListener(expectedCount);
    eventBus.register(metricsListener);

    return metricsListener;
  }
}
