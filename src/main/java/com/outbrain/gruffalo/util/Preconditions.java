package com.outbrain.gruffalo.util;

import java.util.Objects;

public class Preconditions {

  public static void checkArgument(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  public static <T> T checkNotNull(T reference, Object errorMessage) {
    return Objects.requireNonNull(reference, String.valueOf(errorMessage));
  }

}
