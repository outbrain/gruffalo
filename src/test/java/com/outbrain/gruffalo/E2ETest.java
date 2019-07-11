package com.outbrain.gruffalo;

import com.outbrain.gruffalo.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * End To End test for the entire server stack.
 * @author Eran Harel
 */
class E2ETest {

  private static final int SERVER_PORT = 3003;
  private MockGraphite mockGraphite;
  private StandaloneGruffaloServer gruffaloServer;

  @BeforeEach
  void setup() throws Exception {
    mockGraphite = new MockGraphite();
    Config config = Config.parseCommand(
        "test.gruffalo",
        new String[]{"-p", String.valueOf(SERVER_PORT), "-c", "localhost:" + mockGraphite.getPort(), "-maxBatchSize", "10"});
    gruffaloServer = new StandaloneGruffaloServer(config);
  }

  @AfterEach
  void teardown() throws Exception {
    gruffaloServer.shutdown();
    mockGraphite.close();
  }

  @Test
  void testGruffaloProxy_singleMetric() throws Exception {
    GraphiteClient client = new GraphiteClient(SERVER_PORT);

    String metric = "0-aaaaaaa";
    client.send(metric);
    client.send(metric);
    client.send("xxx");
    client.close();

    Thread.sleep(100);
    Assertions.assertEquals(2, mockGraphite.getCount("0-aaaaaaa"));

  }

  @Test
  void testGruffaloProxy_concurrentThreads() throws Exception {
    final int numThreads = 4;
    final int numMetrics = 500;
    final String metricFormat = "metric-%d";
    final CountDownLatch wg = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      new Thread(() -> {
        try {
          GraphiteClient client = new GraphiteClient(SERVER_PORT);
          for (int j = 0; j < numMetrics; j++) {
            client.send(String.format(metricFormat, j));
          }

          client.send("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"); // just make sure we send all batches we're interested in
          client.close();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          wg.countDown();
        }
      }).start();
    }

    wg.await();
    Thread.sleep(100);

    for (int i = 0; i < numThreads; i++) {
      Assertions.assertEquals(numThreads, mockGraphite.getCount(String.format(metricFormat, i)));
    }
  }


//  private static class ClientThread implements Runnable {
//
//    @Override
//    public void run() {
//
//    }
//  }
}
