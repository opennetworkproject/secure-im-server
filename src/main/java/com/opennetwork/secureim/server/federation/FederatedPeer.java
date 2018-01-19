package com.opennetwork.secureim.server.federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

public class FederatedPeer {

  @NotEmpty
  @JsonProperty
  private String name;

  @NotEmpty
  @URL
  @JsonProperty
  private String url;

  @NotEmpty
  @JsonProperty
  private String authenticationToken;

  @NotEmpty
  @JsonProperty
  private String certificate;

  public FederatedPeer() {}

  public FederatedPeer(String name, String url, String authenticationToken, String certificate) {
    this.name                = name;
    this.url                 = url;
    this.authenticationToken = authenticationToken;
    this.certificate         = certificate;
  }

  public String getUrl() {
    return url;
  }

  public String getName() {
    return name;
  }

  public String getAuthenticationToken() {
    return authenticationToken;
  }

  public String getCertificate() {
    return certificate;
  }
}
