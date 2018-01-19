package com.opennetwork.secureim.server.controllers;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.opennetwork.secureim.server.storage.Account;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.util.Util;
import com.opennetwork.secureim.server.federation.FederatedPeer;
import com.opennetwork.secureim.server.federation.NonLimitedAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.server.entities.AccountCount;
import com.opennetwork.secureim.server.entities.AttachmentUri;
import com.opennetwork.secureim.server.entities.ClientContact;
import com.opennetwork.secureim.server.entities.ClientContacts;
import com.opennetwork.secureim.server.entities.IncomingMessageList;
import com.opennetwork.secureim.server.federation.FederatedPeer;
import com.opennetwork.secureim.server.federation.NonLimitedAccount;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import io.dropwizard.auth.Auth;

@Path("/v1/federation")
public class FederationControllerV1 extends FederationController {

  private final Logger logger = LoggerFactory.getLogger(FederationControllerV1.class);

  private static final int ACCOUNT_CHUNK_SIZE = 10000;

  public FederationControllerV1(AccountsManager accounts,
                                AttachmentController attachmentController,
                                MessageController messageController)
  {
    super(accounts, attachmentController, messageController);
  }

  @Timed
  @GET
  @Path("/attachment/{attachmentId}")
  @Produces(MediaType.APPLICATION_JSON)
  public AttachmentUri getSignedAttachmentUri(@Auth FederatedPeer peer,
                                              @PathParam("attachmentId") long attachmentId)
      throws IOException
  {
    return attachmentController.redirectToAttachment(new NonLimitedAccount("Unknown", -1, peer.getName()),
                                                     attachmentId, Optional.<String>absent());
  }

  @Timed
  @PUT
  @Path("/messages/{source}/{sourceDeviceId}/{destination}")
  public void sendMessages(@Auth                        FederatedPeer peer,
                           @PathParam("source")         String source,
                           @PathParam("sourceDeviceId") long sourceDeviceId,
                           @PathParam("destination")    String destination,
                           @Valid IncomingMessageList messages)
      throws IOException
  {
    try {
      messages.setRelay(null);
      messageController.sendMessage(new NonLimitedAccount(source, sourceDeviceId, peer.getName()), destination, messages);
    } catch (RateLimitExceededException e) {
      logger.warn("Rate limiting on federated channel", e);
      throw new IOException(e);
    }
  }

  @Timed
  @GET
  @Path("/user_count")
  @Produces(MediaType.APPLICATION_JSON)
  public AccountCount getUserCount(@Auth FederatedPeer peer) {
    return new AccountCount((int)accounts.getCount());
  }

  @Timed
  @GET
  @Path("/user_tokens/{offset}")
  @Produces(MediaType.APPLICATION_JSON)
  public ClientContacts getUserTokens(@Auth                FederatedPeer peer,
                                      @PathParam("offset") int offset)
  {
    List<Account>       accountList    = accounts.getAll(offset, ACCOUNT_CHUNK_SIZE);
    List<ClientContact> clientContacts = new LinkedList<>();

    for (Account account : accountList) {
      byte[]        token         = Util.getContactToken(account.getNumber());
      ClientContact clientContact = new ClientContact(token, null, account.isVoiceSupported(), account.isVideoSupported());

      if (!account.isActive()) {
        clientContact.setInactive(true);
      }

      clientContacts.add(clientContact);
    }

    return new ClientContacts(clientContacts);
  }
}
