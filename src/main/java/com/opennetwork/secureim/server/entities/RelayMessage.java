
package com.opennetwork.secureim.server.entities;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.opennetwork.secureim.server.util.ByteArrayAdapter;
import com.opennetwork.secureim.server.util.ByteArrayAdapter;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class RelayMessage {

  @JsonProperty
  @NotEmpty
  private String destination;

  @JsonProperty
  @NotEmpty
  private long destinationDeviceId;

  @JsonProperty
  @NotNull
  @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
  @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
  private byte[] outgoingMessageSignal;

  public RelayMessage() {}

  public RelayMessage(String destination, long destinationDeviceId, byte[] outgoingMessageSignal) {
    this.destination           = destination;
    this.destinationDeviceId   = destinationDeviceId;
    this.outgoingMessageSignal = outgoingMessageSignal;
  }

  public String getDestination() {
    return destination;
  }

  public long getDestinationDeviceId() {
    return destinationDeviceId;
  }

  public byte[] getOutgoingMessageSignal() {
    return outgoingMessageSignal;
  }
}
