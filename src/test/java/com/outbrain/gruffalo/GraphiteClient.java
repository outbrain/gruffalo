package com.outbrain.gruffalo;

import io.netty.util.CharsetUtil;

import javax.net.SocketFactory;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

/**
 * A simple "graphite" client suitable for unit tests.
 * The client writes and flushes any "metric" passed to it to the server running on localhost, and the provided port,
 * and appends a new line to each metric.
 * @author Eran Harel
 */
class GraphiteClient {
  private final Socket socket;
  private final Writer writer;

  GraphiteClient(final int port) throws IOException {
    this.socket = SocketFactory.getDefault().createSocket("127.0.0.1", port);
    this.writer = new OutputStreamWriter(this.socket.getOutputStream(), CharsetUtil.UTF_8);
  }

  void send(String metric) throws IOException {
    writer.write(metric + "\n");
    writer.flush();
  }

  void close() throws IOException {
    writer.close();
    socket.close();
  }
}
