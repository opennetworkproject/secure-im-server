package com.opennetwork.secureim.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

public class MessageStoreConfiguration {
  @JsonProperty
  @NotEmpty
  private String url;

  public String getUrl() {
    return url;
  }
}
