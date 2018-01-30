package com.opennetwork.secureim.server;

import com.codahale.metrics.SharedMetricRegistries;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.base.Optional;
import com.opennetwork.secureim.dispatch.DispatchManager;
import com.opennetwork.secureim.server.auth.TurnTokenGenerator;
import com.opennetwork.secureim.server.controllers.ReceiptController;
import com.opennetwork.secureim.server.providers.RedisClientFactory;
import com.opennetwork.secureim.server.push.ReceiptSender;
import com.opennetwork.secureim.dispatch.DispatchChannel;
import com.opennetwork.secureim.server.federation.FederatedClientManager;
import com.opennetwork.secureim.server.federation.FederatedPeer;
import com.opennetwork.secureim.server.providers.RedisHealthCheck;
import com.opennetwork.secureim.server.providers.TimeProvider;
import com.opennetwork.secureim.server.websocket.AuthenticatedConnectListener;
import com.opennetwork.secureim.server.websocket.DeadLetterHandler;
import com.opennetwork.secureim.server.websocket.ProvisioningConnectListener;
import com.opennetwork.secureim.server.websocket.WebSocketAccountAuthenticator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.skife.jdbi.v2.DBI;
import com.opennetwork.secureim.auth.AuthDynamicFeature;
import com.opennetwork.secureim.auth.AuthValueFactoryProvider;
import com.opennetwork.secureim.auth.BasicCredentialAuthFilter;
import com.opennetwork.secureim.server.auth.AccountAuthenticator;
import com.opennetwork.secureim.server.auth.FederatedPeerAuthenticator;
import com.opennetwork.secureim.server.controllers.AccountController;
import com.opennetwork.secureim.server.controllers.AttachmentController;
import com.opennetwork.secureim.server.controllers.DeviceController;
import com.opennetwork.secureim.server.controllers.DirectoryController;
import com.opennetwork.secureim.server.controllers.FederationControllerV1;
import com.opennetwork.secureim.server.controllers.FederationControllerV2;
import com.opennetwork.secureim.server.controllers.KeepAliveController;
import com.opennetwork.secureim.server.controllers.KeysController;
import com.opennetwork.secureim.server.controllers.MessageController;
import com.opennetwork.secureim.server.controllers.ProfileController;
import com.opennetwork.secureim.server.controllers.ProvisioningController;
import com.opennetwork.secureim.server.limits.RateLimiters;
import com.opennetwork.secureim.server.liquibase.NameableMigrationsBundle;
import com.opennetwork.secureim.server.mappers.DeviceLimitExceededExceptionMapper;
import com.opennetwork.secureim.server.mappers.IOExceptionMapper;
import com.opennetwork.secureim.server.mappers.InvalidWebsocketAddressExceptionMapper;
import com.opennetwork.secureim.server.mappers.RateLimitExceededExceptionMapper;
import com.opennetwork.secureim.server.metrics.CpuUsageGauge;
import com.opennetwork.secureim.server.metrics.FileDescriptorGauge;
import com.opennetwork.secureim.server.metrics.FreeMemoryGauge;
import com.opennetwork.secureim.server.metrics.NetworkReceivedGauge;
import com.opennetwork.secureim.server.metrics.NetworkSentGauge;
import com.opennetwork.secureim.server.push.APNSender;
import com.opennetwork.secureim.server.push.ApnFallbackManager;
import com.opennetwork.secureim.server.push.GCMSender;
import com.opennetwork.secureim.server.push.PushSender;
import com.opennetwork.secureim.server.push.WebsocketSender;
import com.opennetwork.secureim.server.s3.UrlSigner;
import com.opennetwork.secureim.server.sms.SmsSender;
import com.opennetwork.secureim.server.sms.TwilioSmsSender;
import com.opennetwork.secureim.server.storage.Account;
import com.opennetwork.secureim.server.storage.Accounts;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.storage.DirectoryManager;
import com.opennetwork.secureim.server.storage.Keys;
import com.opennetwork.secureim.server.storage.Messages;
import com.opennetwork.secureim.server.storage.MessagesManager;
import com.opennetwork.secureim.server.storage.PendingAccounts;
import com.opennetwork.secureim.server.storage.PendingAccountsManager;
import com.opennetwork.secureim.server.storage.PendingDevices;
import com.opennetwork.secureim.server.storage.PendingDevicesManager;
import com.opennetwork.secureim.server.storage.PubSubManager;
import com.opennetwork.secureim.server.util.Constants;
import com.opennetwork.secureim.server.workers.DeleteUserCommand;
import com.opennetwork.secureim.server.workers.DirectoryCommand;
import com.opennetwork.secureim.server.workers.PeriodicStatsCommand;
import com.opennetwork.secureim.server.workers.TrimMessagesCommand;
import com.opennetwork.secureim.server.workers.VacuumCommand;
import com.opennetwork.secureim.websocket.WebSocketResourceProviderFactory;
import com.opennetwork.secureim.websocket.setup.WebSocketEnvironment;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import java.security.Security;
import java.util.EnumSet;

import static com.codahale.metrics.MetricRegistry.name;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import redis.clients.jedis.JedisPool;

public class SecureImServerService extends Application<SecureImServerConfiguration> {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  @Override
  public void initialize(Bootstrap<SecureImServerConfiguration> bootstrap) {
    bootstrap.addCommand(new DirectoryCommand());
    bootstrap.addCommand(new VacuumCommand());
    bootstrap.addCommand(new TrimMessagesCommand());
    bootstrap.addCommand(new PeriodicStatsCommand());
    bootstrap.addCommand(new DeleteUserCommand());
    bootstrap.addBundle(new NameableMigrationsBundle<SecureImServerConfiguration>("accountdb", "accountsdb.xml") {
      @Override
      public DataSourceFactory getDataSourceFactory(SecureImServerConfiguration configuration) {
        return configuration.getDataSourceFactory();
      }
    });

    bootstrap.addBundle(new NameableMigrationsBundle<SecureImServerConfiguration>("messagedb", "messagedb.xml") {
      @Override
      public DataSourceFactory getDataSourceFactory(SecureImServerConfiguration configuration) {
        return configuration.getMessageStoreConfiguration();
      }
    });
  }

  @Override
  public String getName() {
    return "secure-im-server";
  }

  @Override
  public void run(SecureImServerConfiguration config, Environment environment)
      throws Exception
  {
    SharedMetricRegistries.add(Constants.METRICS_NAME, environment.metrics());
    environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    environment.getObjectMapper().setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    environment.getObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    DBIFactory dbiFactory = new DBIFactory();
    DBI        database   = dbiFactory.build(environment, config.getDataSourceFactory(), "accountdb");
    DBI        messagedb  = dbiFactory.build(environment, config.getMessageStoreConfiguration(), "messagedb");

    Accounts        accounts        = database.onDemand(Accounts.class);
    PendingAccounts pendingAccounts = database.onDemand(PendingAccounts.class);
    PendingDevices  pendingDevices  = database.onDemand(PendingDevices.class);
    Keys            keys            = database.onDemand(Keys.class);
    Messages        messages        = messagedb.onDemand(Messages.class);

    RedisClientFactory cacheClientFactory = new RedisClientFactory(config.getCacheConfiguration().getUrl());
    JedisPool          cacheClient        = cacheClientFactory.getRedisClientPool();
    JedisPool          directoryClient    = new RedisClientFactory(config.getDirectoryConfiguration().getUrl()).getRedisClientPool();

    DirectoryManager           directory                  = new DirectoryManager(directoryClient);
    PendingAccountsManager     pendingAccountsManager     = new PendingAccountsManager(pendingAccounts, cacheClient);
    PendingDevicesManager      pendingDevicesManager      = new PendingDevicesManager (pendingDevices, cacheClient );
    AccountsManager            accountsManager            = new AccountsManager(accounts, directory, cacheClient);
    FederatedClientManager federatedClientManager     = new FederatedClientManager(environment, config.getJerseyClientConfiguration(), config.getFederationConfiguration());
    MessagesManager            messagesManager            = new MessagesManager(messages);
    DeadLetterHandler deadLetterHandler          = new DeadLetterHandler(messagesManager);
    DispatchManager dispatchManager            = new DispatchManager(cacheClientFactory, Optional.<DispatchChannel>of(deadLetterHandler));
    PubSubManager              pubSubManager              = new PubSubManager(cacheClient, dispatchManager);
    APNSender                  apnSender                  = new APNSender(accountsManager, config.getApnConfiguration());
    GCMSender                  gcmSender                  = new GCMSender(accountsManager, config.getGcmConfiguration().getApiKey());
    WebsocketSender            websocketSender            = new WebsocketSender(messagesManager, pubSubManager);
    AccountAuthenticator       deviceAuthenticator        = new AccountAuthenticator(accountsManager                 );
    FederatedPeerAuthenticator federatedPeerAuthenticator = new FederatedPeerAuthenticator(config.getFederationConfiguration());
    RateLimiters               rateLimiters               = new RateLimiters(config.getLimitsConfiguration(), cacheClient);

    ApnFallbackManager       apnFallbackManager  = new ApnFallbackManager(apnSender, pubSubManager);
    TwilioSmsSender          twilioSmsSender     = new TwilioSmsSender(config.getTwilioConfiguration());
    SmsSender                smsSender           = new SmsSender(twilioSmsSender);
    UrlSigner                urlSigner           = new UrlSigner(config.getAttachmentsConfiguration());
    PushSender               pushSender          = new PushSender(apnFallbackManager, gcmSender, apnSender, websocketSender, config.getPushConfiguration().getQueueSize());
    ReceiptSender receiptSender       = new ReceiptSender(accountsManager, pushSender, federatedClientManager);
    TurnTokenGenerator turnTokenGenerator  = new TurnTokenGenerator(config.getTurnConfiguration());
    Optional<byte[]>         authorizationKey    = config.getRedphoneConfiguration().getAuthorizationKey();

    apnSender.setApnFallbackManager(apnFallbackManager);
    environment.lifecycle().manage(apnFallbackManager);
    environment.lifecycle().manage(pubSubManager);
    environment.lifecycle().manage(pushSender);

    AttachmentController attachmentController = new AttachmentController(rateLimiters, federatedClientManager, urlSigner);
    KeysController keysController       = new KeysController(rateLimiters, keys, accountsManager, federatedClientManager);
    MessageController messageController    = new MessageController(rateLimiters, pushSender, receiptSender, accountsManager, messagesManager, federatedClientManager);
    ProfileController profileController    = new ProfileController(rateLimiters , accountsManager, config.getProfilesConfiguration());

    environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<Account>()
                                                             .setAuthenticator(deviceAuthenticator)
                                                             .setPrincipal(Account.class)
                                                             .buildAuthFilter(),
                                                         new BasicCredentialAuthFilter.Builder<FederatedPeer>()
                                                             .setAuthenticator(federatedPeerAuthenticator)
                                                             .setPrincipal(FederatedPeer.class)
                                                             .buildAuthFilter()));
    environment.jersey().register(new AuthValueFactoryProvider.Binder());

    environment.jersey().register(new AccountController(pendingAccountsManager, accountsManager, rateLimiters, smsSender, messagesManager, new TimeProvider(), authorizationKey, turnTokenGenerator, config.getTestDevices()));
    environment.jersey().register(new DeviceController(pendingDevicesManager, accountsManager, messagesManager, rateLimiters, config.getMaxDevices()));
    environment.jersey().register(new DirectoryController(rateLimiters, directory));
    environment.jersey().register(new FederationControllerV1(accountsManager, attachmentController, messageController));
    environment.jersey().register(new FederationControllerV2(accountsManager, attachmentController, messageController, keysController));
    environment.jersey().register(new ReceiptController(receiptSender));
    environment.jersey().register(new ProvisioningController(rateLimiters, pushSender));
    environment.jersey().register(attachmentController);
    environment.jersey().register(keysController);
    environment.jersey().register(messageController);
    environment.jersey().register(profileController);

    ///
    WebSocketEnvironment webSocketEnvironment = new WebSocketEnvironment(environment, config.getWebSocketConfiguration(), 90000);
    webSocketEnvironment.setAuthenticator(new WebSocketAccountAuthenticator(deviceAuthenticator));
    webSocketEnvironment.setConnectListener(new AuthenticatedConnectListener(accountsManager, pushSender, receiptSender, messagesManager, pubSubManager));
    webSocketEnvironment.jersey().register(new KeepAliveController(pubSubManager));
    webSocketEnvironment.jersey().register(messageController);
    webSocketEnvironment.jersey().register(profileController);

    WebSocketEnvironment provisioningEnvironment = new WebSocketEnvironment(environment, webSocketEnvironment.getRequestLog(), 60000);
    provisioningEnvironment.setConnectListener(new ProvisioningConnectListener(pubSubManager));
    provisioningEnvironment.jersey().register(new KeepAliveController(pubSubManager));

    WebSocketResourceProviderFactory webSocketServlet    = new WebSocketResourceProviderFactory(webSocketEnvironment   );
    WebSocketResourceProviderFactory provisioningServlet = new WebSocketResourceProviderFactory(provisioningEnvironment);

    ServletRegistration.Dynamic websocket    = environment.servlets().addServlet("WebSocket", webSocketServlet      );
    ServletRegistration.Dynamic provisioning = environment.servlets().addServlet("Provisioning", provisioningServlet);

    websocket.addMapping("/v1/websocket/");
    websocket.setAsyncSupported(true);

    provisioning.addMapping("/v1/websocket/provisioning/");
    provisioning.setAsyncSupported(true);

    webSocketServlet.start();
    provisioningServlet.start();

    FilterRegistration.Dynamic filter = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    filter.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    filter.setInitParameter("allowedOrigins", "*");
    filter.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,X-Signal-Agent");
    filter.setInitParameter("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS");
    filter.setInitParameter("preflightMaxAge", "5184000");
    filter.setInitParameter("allowCredentials", "true");
///

    environment.healthChecks().register("directory", new RedisHealthCheck(directoryClient));
    environment.healthChecks().register("cache", new RedisHealthCheck(cacheClient));

    environment.jersey().register(new IOExceptionMapper());
    environment.jersey().register(new RateLimitExceededExceptionMapper());
    environment.jersey().register(new InvalidWebsocketAddressExceptionMapper());
    environment.jersey().register(new DeviceLimitExceededExceptionMapper());

    environment.metrics().register(name(CpuUsageGauge.class, "cpu"), new CpuUsageGauge());
    environment.metrics().register(name(FreeMemoryGauge.class, "free_memory"), new FreeMemoryGauge());
    environment.metrics().register(name(NetworkSentGauge.class, "bytes_sent"), new NetworkSentGauge());
    environment.metrics().register(name(NetworkReceivedGauge.class, "bytes_received"), new NetworkReceivedGauge());
    environment.metrics().register(name(FileDescriptorGauge.class, "fd_count"), new FileDescriptorGauge());
  }

  public static void main(String[] args) throws Exception {
    new SecureImServerService().run(args);
  }
}
