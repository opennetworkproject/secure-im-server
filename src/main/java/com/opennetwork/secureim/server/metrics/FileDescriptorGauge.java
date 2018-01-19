package com.opennetwork.secureim.server.metrics;


import com.codahale.metrics.Gauge;

import java.io.File;

public class FileDescriptorGauge implements Gauge<Integer> {
  @Override
  public Integer getValue() {
    File file = new File("/proc/self/fd");

    if (file.isDirectory() && file.exists()) {
      return file.list().length;
    }

    return 0;
  }
}
