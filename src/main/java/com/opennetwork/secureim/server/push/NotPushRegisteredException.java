package com.opennetwork.secureim.server.push;

public class NotPushRegisteredException extends Exception {
  public NotPushRegisteredException(String s) {
    super(s);
  }

  public NotPushRegisteredException(Exception e) {
    super(e);
  }
}
