package com.opennetwork.secureim.websocket.setup;

import com.opennetwork.secureim.websocket.session.WebSocketSessionContext;

public interface WebSocketConnectListener {
  public void onWebSocketConnect(WebSocketSessionContext context);
}
