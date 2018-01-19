package com.opennetwork.secureim.server.controllers;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.federation.FederatedPeer;
import com.opennetwork.secureim.server.federation.NonLimitedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.server.entities.PreKeyResponse;
import com.opennetwork.secureim.server.federation.FederatedPeer;
import com.opennetwork.secureim.server.federation.NonLimitedAccount;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import io.dropwizard.auth.Auth;

@Path("/v2/federation")
public class FederationControllerV2 extends FederationController {

  private final Logger logger = LoggerFactory.getLogger(FederationControllerV2.class);

  private final KeysController keysController;

  public FederationControllerV2(AccountsManager accounts, AttachmentController attachmentController, MessageController messageController, KeysController keysController) {
    super(accounts, attachmentController, messageController);
    this.keysController = keysController;
  }

  @Timed
  @GET
  @Path("/key/{number}/{device}")
  @Produces(MediaType.APPLICATION_JSON)
  public Optional<PreKeyResponse> getKeysV2(@Auth FederatedPeer peer,
                                            @PathParam("number") String number,
                                            @PathParam("device") String device)
      throws IOException
  {
    try {
      return keysController.getDeviceKeys(new NonLimitedAccount("Unknown", -1, peer.getName()),
                                          number, device, Optional.<String>absent());
    } catch (RateLimitExceededException e) {
      logger.warn("Rate limiting on federated channel", e);
      throw new IOException(e);
    }
  }

}
