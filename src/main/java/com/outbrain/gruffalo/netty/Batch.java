package com.outbrain.gruffalo.netty;

import io.netty.buffer.ByteBuf;

/**
 * Time: 8/5/13 11:23 AM
 *
 * @author Eran Harel
 */
class Batch {
  final ByteBuf payload;
  final int batchSize;

  Batch(ByteBuf payload, int batchSize) {
    this.payload = payload;
    this.batchSize = batchSize;
  }
}
