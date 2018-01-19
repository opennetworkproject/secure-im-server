package com.opennetwork.secureim.server.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class MaxDeviceConfiguration {

  @JsonProperty
  @NotEmpty
  private String number;

  @JsonProperty
  @NotNull
  private int count;

  public String getNumber() {
    return number;
  }

  public int getCount() {
    return count;
  }

}
