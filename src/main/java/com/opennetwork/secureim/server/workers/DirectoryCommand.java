package com.opennetwork.secureim.server.workers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.opennetwork.secureim.server.providers.RedisClientFactory;
import com.opennetwork.secureim.server.SecureImServerConfiguration;
import com.opennetwork.secureim.server.providers.RedisClientFactory;
import com.opennetwork.secureim.server.storage.Accounts;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.storage.DirectoryManager;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.server.SecureImServerConfiguration;
import com.opennetwork.secureim.server.storage.Accounts;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.storage.DirectoryManager;

import io.dropwizard.Application;
import io.dropwizard.cli.EnvironmentCommand;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.ImmutableListContainerFactory;
import io.dropwizard.jdbi.ImmutableSetContainerFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.jdbi.args.OptionalArgumentFactory;
import io.dropwizard.setup.Environment;
import redis.clients.jedis.JedisPool;

public class DirectoryCommand extends EnvironmentCommand<SecureImServerConfiguration> {

  private final Logger logger = LoggerFactory.getLogger(DirectoryCommand.class);

  public DirectoryCommand() {
    super(new Application<SecureImServerConfiguration>() {
      @Override
      public void run(SecureImServerConfiguration configuration, Environment environment)
          throws Exception
      {

      }
    }, "directory", "Update directory from DB and peers.");
  }

  @Override
  protected void run(Environment environment, Namespace namespace,
                     SecureImServerConfiguration configuration)
      throws Exception
  {
    try {
      environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      DataSourceFactory dbConfig = configuration.getReadDataSourceFactory();
      DBI               dbi      = new DBI(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword());

      dbi.registerArgumentFactory(new OptionalArgumentFactory(dbConfig.getDriverClass()));
      dbi.registerContainerFactory(new ImmutableListContainerFactory());
      dbi.registerContainerFactory(new ImmutableSetContainerFactory());
      dbi.registerContainerFactory(new OptionalContainerFactory());

      Accounts accounts               = dbi.onDemand(Accounts.class);
      JedisPool              cacheClient            = new RedisClientFactory(configuration.getCacheConfiguration().getUrl()).getRedisClientPool();
      JedisPool              redisClient            = new RedisClientFactory(configuration.getDirectoryConfiguration().getUrl()).getRedisClientPool();
      DirectoryManager directory              = new DirectoryManager(redisClient);
      AccountsManager accountsManager        = new AccountsManager(accounts, directory, cacheClient);
//      FederatedClientManager federatedClientManager = new FederatedClientManager(environment,
//                                                                                 configuration.getJerseyClientConfiguration(),
//                                                                                 configuration.getFederationConfiguration());

      DirectoryUpdater update = new DirectoryUpdater(accountsManager, directory);

      update.updateFromLocalDatabase();
//      update.updateFromPeers();
    } catch (Exception ex) {
      logger.warn("Directory Exception", ex);
      throw new RuntimeException(ex);
    } finally {
//      Thread.sleep(3000);
//      System.exit(0);
    }
  }
}
