package com.opennetwork.secureim.server.push;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.SharedMetricRegistries;
import com.opennetwork.secureim.server.entities.MessageProtos;
import com.opennetwork.secureim.server.storage.Account;
import com.opennetwork.secureim.server.storage.Device;
import com.opennetwork.secureim.server.util.BlockingThreadPoolExecutor;
import com.opennetwork.secureim.server.util.Constants;
import com.opennetwork.secureim.server.util.Util;
import com.opennetwork.secureim.server.websocket.WebsocketAddress;
import com.opennetwork.secureim.server.websocket.WebsocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import io.dropwizard.lifecycle.Managed;

public class PushSender implements Managed {

  private final Logger logger = LoggerFactory.getLogger(PushSender.class);

  public static final String APN_PAYLOAD = "{\"aps\":{\"sound\":\"default\",\"badge\":%d,\"alert\":{\"loc-key\":\"APN_Message\"}}}";

  private final ApnFallbackManager         apnFallbackManager;
  private final GCMSender                  gcmSender;
  private final APNSender                  apnSender;
  private final WebsocketSender            webSocketSender;
  private final BlockingThreadPoolExecutor executor;
  private final int                        queueSize;

  public PushSender(ApnFallbackManager apnFallbackManager,
                    GCMSender gcmSender, APNSender apnSender,
                    WebsocketSender websocketSender, int queueSize)
  {
    this.apnFallbackManager = apnFallbackManager;
    this.gcmSender          = gcmSender;
    this.apnSender          = apnSender;
    this.webSocketSender    = websocketSender;
    this.queueSize          = queueSize;
    this.executor           = new BlockingThreadPoolExecutor(50, queueSize);

    SharedMetricRegistries.getOrCreate(Constants.METRICS_NAME)
                          .register(name(PushSender.class, "send_queue_depth"),
                                    new Gauge<Integer>() {
                                      @Override
                                      public Integer getValue() {
                                        return executor.getSize();
                                      }
                                    });
  }

  public void sendMessage(final Account account, final Device device, final MessageProtos.Envelope message, final boolean silent)
      throws NotPushRegisteredException
  {
    if (device.getGcmId() == null && device.getApnId() == null && !device.getFetchesMessages()) {
      throw new NotPushRegisteredException("No delivery possible!");
    }

    if (queueSize > 0) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          sendSynchronousMessage(account, device, message, silent);
        }
      });
    } else {
      sendSynchronousMessage(account, device, message, silent);
    }
  }

  public void sendQueuedNotification(Account account, Device device, int messageQueueDepth, boolean fallback)
      throws NotPushRegisteredException, TransientPushFailureException
  {
    if      (device.getGcmId() != null)    sendGcmNotification(account, device);
    else if (device.getApnId() != null)    sendApnNotification(account, device, messageQueueDepth, fallback);
    else if (!device.getFetchesMessages()) throw new NotPushRegisteredException("No notification possible!");
  }

  public WebsocketSender getWebSocketSender() {
    return webSocketSender;
  }

  private void sendSynchronousMessage(Account account, Device device, MessageProtos.Envelope message, boolean silent) {
    if      (device.getGcmId() != null)   sendGcmMessage(account, device, message);
    else if (device.getApnId() != null)   sendApnMessage(account, device, message, silent);
    else if (device.getFetchesMessages()) sendWebSocketMessage(account, device, message);
    else                                  throw new AssertionError();
  }

  private void sendGcmMessage(Account account, Device device, MessageProtos.Envelope message) {
    WebsocketSender.DeliveryStatus deliveryStatus = webSocketSender.sendMessage(account, device, message, WebsocketSender.Type.GCM);

    if (!deliveryStatus.isDelivered()) {
      sendGcmNotification(account, device);
    }
  }

  private void sendGcmNotification(Account account, Device device) {
    GcmMessage gcmMessage = new GcmMessage(device.getGcmId(), account.getNumber(),
                                           (int)device.getId(), false);

    gcmSender.sendMessage(gcmMessage);
  }

  private void sendApnMessage(Account account, Device device, MessageProtos.Envelope outgoingMessage, boolean silent) {
    WebsocketSender.DeliveryStatus deliveryStatus = webSocketSender.sendMessage(account, device, outgoingMessage, WebsocketSender.Type.APN);

    if (!deliveryStatus.isDelivered() && outgoingMessage.getType() != MessageProtos.Envelope.Type.RECEIPT) {
      boolean fallback = !silent && !outgoingMessage.getSource().equals(account.getNumber());
      sendApnNotification(account, device, deliveryStatus.getMessageQueueDepth(), fallback);
    }
  }

  private void sendApnNotification(Account account, Device device, int messageQueueDepth, boolean fallback) {
    ApnMessage apnMessage;

    if (!Util.isEmpty(device.getVoipApnId())) {
      apnMessage = new ApnMessage(device.getVoipApnId(), account.getNumber(), (int)device.getId(),
                                  String.format(APN_PAYLOAD, messageQueueDepth), true,
                                  System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ApnFallbackManager.FALLBACK_DURATION));

      if (fallback) {
        apnFallbackManager.schedule(new WebsocketAddress(account.getNumber(), device.getId()),
                                    new ApnFallbackManager.ApnFallbackTask(device.getApnId(), device.getVoipApnId(), apnMessage));
      }
    } else {
      apnMessage = new ApnMessage(device.getApnId(), account.getNumber(), (int)device.getId(),
                                  String.format(APN_PAYLOAD, messageQueueDepth),
                                  false, ApnMessage.MAX_EXPIRATION);
    }

    try {
      apnSender.sendMessage(apnMessage);
    } catch (TransientPushFailureException e) {
      logger.warn("SILENT PUSH LOSS", e);
    }
  }

  private void sendWebSocketMessage(Account account, Device device, MessageProtos.Envelope outgoingMessage)
  {
    webSocketSender.sendMessage(account, device, outgoingMessage, WebsocketSender.Type.WEB);
  }

  @Override
  public void start() throws Exception {
    apnSender.start();
    gcmSender.start();
  }

  @Override
  public void stop() throws Exception {
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES);

    apnSender.stop();
    gcmSender.stop();
  }
}
