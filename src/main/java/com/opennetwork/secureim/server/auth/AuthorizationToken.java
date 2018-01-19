package com.opennetwork.secureim.server.auth;


import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationToken {

  @JsonProperty
  private String token;

  public AuthorizationToken(String token) {
    this.token = token;
  }

  public AuthorizationToken() {}

}
