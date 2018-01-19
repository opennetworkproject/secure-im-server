package com.opennetwork.secureim.server.controllers;

import java.util.List;


public class StaleDevicesException extends Throwable {
  private final List<Long> staleDevices;

  public StaleDevicesException(List<Long> staleDevices) {
    this.staleDevices = staleDevices;
  }

  public List<Long> getStaleDevices() {
    return staleDevices;
  }
}
