package com.opennetwork.secureim.server.metrics;

import com.codahale.metrics.Gauge;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public class CpuUsageGauge implements Gauge<Integer> {
  @Override
  public Integer getValue() {
    OperatingSystemMXBean mbean = (OperatingSystemMXBean)
        ManagementFactory.getOperatingSystemMXBean();

    return (int) Math.ceil(mbean.getSystemCpuLoad() * 100);
  }
}