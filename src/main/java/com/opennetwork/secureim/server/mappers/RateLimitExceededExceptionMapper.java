package com.opennetwork.secureim.server.mappers;

import com.opennetwork.secureim.server.controllers.RateLimitExceededException;
import com.opennetwork.secureim.server.controllers.RateLimitExceededException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RateLimitExceededExceptionMapper implements ExceptionMapper<RateLimitExceededException> {
  @Override
  public Response toResponse(RateLimitExceededException e) {
    return Response.status(413).build();
  }
}
