package com.opennetwork.secureim.server.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ClientContactTokens {

  @NotNull
  @JsonProperty
  private List<String> contacts;

  public List<String> getContacts() {
    return contacts;
  }

  public ClientContactTokens() {}

  public ClientContactTokens(List<String> contacts) {
    this.contacts = contacts;
  }

}
