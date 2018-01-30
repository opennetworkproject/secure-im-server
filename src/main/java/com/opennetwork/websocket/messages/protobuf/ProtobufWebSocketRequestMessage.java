package com.opennetwork.websocket.messages.protobuf;

import com.google.common.base.Optional;
import com.opennetwork.websocket.messages.WebSocketRequestMessage;

import java.util.HashMap;
import java.util.Map;

public class ProtobufWebSocketRequestMessage implements WebSocketRequestMessage {

  private final SubProtocol.WebSocketRequestMessage message;

  ProtobufWebSocketRequestMessage(SubProtocol.WebSocketRequestMessage message) {
    this.message = message;
  }

  @Override
  public String getVerb() {
    return message.getVerb();
  }

  @Override
  public String getPath() {
    return message.getPath();
  }

  @Override
  public Optional<byte[]> getBody() {
    if (message.hasBody()) {
      return Optional.of(message.getBody().toByteArray());
    } else {
      return Optional.absent();
    }
  }

  @Override
  public long getRequestId() {
    return message.getId();
  }

  @Override
  public boolean hasRequestId() {
    return message.hasId();
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> results = new HashMap<>();

    for (String header : message.getHeadersList()) {
      String[] tokenized = header.split(":");

      if (tokenized.length == 2 && tokenized[0] != null && tokenized[1] != null) {
        results.put(tokenized[0].trim().toLowerCase(), tokenized[1].trim());
      }
    }

    return results;
  }
}
