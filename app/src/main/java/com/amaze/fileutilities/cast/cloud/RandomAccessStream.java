/*
 * Copyright (C) 2021-2020 Team Amaze - Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com>. All Rights reserved.
 *
 * This file is part of Amaze File Utilities.
 *
 * 'Amaze File Utilities' is a registered trademark of Team Amaze. All other product
 * and company names mentioned are trademarks or registered trademarks of their respective owners.
 */

package com.amaze.fileutilities.cast.cloud;

import java.io.IOException;
import java.io.InputStream;

public abstract class RandomAccessStream extends InputStream {

  private long markedPosition;
  private long length;

  public RandomAccessStream(long length) {
    this.length = length;

    mark(-1);
  }

  @Override
  public synchronized void reset() {
    moveTo(markedPosition);
  }

  @Override
  public synchronized void mark(int readLimit) {
    if (readLimit != -1) {
      throw new IllegalArgumentException(
          "readLimit argument of RandomAccessStream.mark() is not used, please set to -1!");
    }

    markedPosition = getCurrentPosition();
  }

  @Override
  public boolean markSupported() {
    return true;
  }

  public long availableExact() {
    return length - getCurrentPosition();
  }

  public long length() {
    return length;
  }

  @Override
  public int available() throws IOException {
    throw new IOException("Use availableExact()!");
  }

  public abstract int read() throws IOException;

  public abstract void moveTo(long position);

  protected abstract long getCurrentPosition();
}
