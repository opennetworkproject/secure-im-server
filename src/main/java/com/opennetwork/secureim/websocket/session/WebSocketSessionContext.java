package com.opennetwork.secureim.websocket.session;

import com.opennetwork.secureim.websocket.WebSocketClient;

import java.util.LinkedList;
import java.util.List;

public class WebSocketSessionContext {

  private final List<WebSocketEventListener> closeListeners = new LinkedList<>();

  private final WebSocketClient webSocketClient;

  private Object authenticated;
  private boolean closed;

  public WebSocketSessionContext(WebSocketClient webSocketClient) {
    this.webSocketClient = webSocketClient;
  }

  public void setAuthenticated(Object authenticated) {
    this.authenticated = authenticated;
  }

  public <T> T getAuthenticated(Class<T> clazz) {
    if (authenticated != null && clazz.equals(authenticated.getClass())) {
      return clazz.cast(authenticated);
    }

    throw new IllegalArgumentException("No authenticated type for: " + clazz);
  }

  public synchronized void addListener(WebSocketEventListener listener) {
    if (!closed) this.closeListeners.add(listener);
    else         listener.onWebSocketClose(this, 1000, "Closed");
  }

  public WebSocketClient getClient() {
    return webSocketClient;
  }

  public synchronized void notifyClosed(int statusCode, String reason) {
    for (WebSocketEventListener listener : closeListeners) {
      listener.onWebSocketClose(this, statusCode, reason);
    }

    closed = true;
  }

  public interface WebSocketEventListener {
    public void onWebSocketClose(WebSocketSessionContext context, int statusCode, String reason);
  }


}
