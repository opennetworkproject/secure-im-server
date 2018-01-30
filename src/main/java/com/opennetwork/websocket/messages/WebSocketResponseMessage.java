package com.opennetwork.websocket.messages;

import com.google.common.base.Optional;

import java.util.Map;

public interface WebSocketResponseMessage {
  public long               getRequestId();
  public int                getStatus();
  public String             getMessage();
  public Map<String,String> getHeaders();
  public Optional<byte[]>   getBody();
}
