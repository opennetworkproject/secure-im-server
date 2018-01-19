package com.opennetwork.secureim.server.websocket;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.dispatch.DispatchChannel;
import com.opennetwork.secureim.server.entities.MessageProtos.Envelope;
import com.opennetwork.secureim.server.storage.MessagesManager;
import com.opennetwork.secureim.server.storage.PubSubProtos.PubSubMessage;

public class DeadLetterHandler implements DispatchChannel {

  private final Logger logger = LoggerFactory.getLogger(DeadLetterHandler.class);

  private final MessagesManager messagesManager;

  public DeadLetterHandler(MessagesManager messagesManager) {
    this.messagesManager = messagesManager;
  }

  @Override
  public void onDispatchMessage(String channel, byte[] data) {
    if (!WebSocketConnectionInfo.isType(channel)) {
      try {
        logger.info("Handling dead letter to: " + channel);

        WebsocketAddress address       = new WebsocketAddress(channel);
        PubSubMessage    pubSubMessage = PubSubMessage.parseFrom(data);

        switch (pubSubMessage.getType().getNumber()) {
          case PubSubMessage.Type.DELIVER_VALUE:
            Envelope message = Envelope.parseFrom(pubSubMessage.getContent());
            messagesManager.insert(address.getNumber(), address.getDeviceId(), message);
            break;
        }
      } catch (InvalidProtocolBufferException e) {
        logger.warn("Bad pubsub message", e);
      } catch (InvalidWebsocketAddressException e) {
        logger.warn("Invalid websocket address", e);
      }
    }
  }

  @Override
  public void onDispatchSubscribed(String channel) {
    logger.warn("DeadLetterHandler subscription notice! " + channel);
  }

  @Override
  public void onDispatchUnsubscribed(String channel) {
    logger.warn("DeadLetterHandler unsubscribe notice! " + channel);
  }
}
