package com.opennetwork.secureim.server.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;

public class Profile {

  @JsonProperty
  private String identityKey;

  @JsonProperty
  private String name;

  @JsonProperty
  private String avatar;

  public Profile() {}

  public Profile(String name, String avatar, String identityKey) {
    this.name        = name;
    this.avatar      = avatar;
    this.identityKey = identityKey;
  }

  @VisibleForTesting
  public String getIdentityKey() {
    return identityKey;
  }

  @VisibleForTesting
  public String getName() {
    return name;
  }

  @VisibleForTesting
  public String getAvatar() {
    return avatar;
  }

}
