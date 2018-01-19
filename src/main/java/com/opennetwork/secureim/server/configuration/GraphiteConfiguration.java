package com.opennetwork.secureim.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphiteConfiguration {
  @JsonProperty
  private String host;

  @JsonProperty
  private int port;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public boolean isEnabled() {
    return host != null && port != 0;
  }
}
