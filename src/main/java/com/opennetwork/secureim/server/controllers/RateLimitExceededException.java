package com.opennetwork.secureim.server.controllers;

public class RateLimitExceededException extends Exception {
  public RateLimitExceededException(String number) {
    super(number);
  }
}
