package com.opennetwork.secureim.server.workers;

import com.opennetwork.secureim.server.storage.Accounts;
import com.opennetwork.secureim.server.storage.Keys;
import com.opennetwork.secureim.server.storage.PendingAccounts;
import com.opennetwork.secureim.server.SecureImServerConfiguration;
import com.opennetwork.secureim.server.storage.Accounts;
import com.opennetwork.secureim.server.storage.Keys;
import com.opennetwork.secureim.server.storage.Messages;
import com.opennetwork.secureim.server.storage.PendingAccounts;
import net.sourceforge.argparse4j.inf.Namespace;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.server.SecureImServerConfiguration;
import com.opennetwork.secureim.server.storage.Messages;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.ImmutableListContainerFactory;
import io.dropwizard.jdbi.ImmutableSetContainerFactory;
import io.dropwizard.jdbi.OptionalContainerFactory;
import io.dropwizard.jdbi.args.OptionalArgumentFactory;
import io.dropwizard.setup.Bootstrap;


public class VacuumCommand extends ConfiguredCommand<SecureImServerConfiguration> {

  private final Logger logger = LoggerFactory.getLogger(VacuumCommand.class);

  public VacuumCommand() {
    super("vacuum", "Vacuum Postgres Tables");
  }

  @Override
  protected void run(Bootstrap<SecureImServerConfiguration> bootstrap,
                     Namespace namespace,
                     SecureImServerConfiguration config)
      throws Exception
  {
    DataSourceFactory dbConfig        = config.getDataSourceFactory();
    DataSourceFactory messageDbConfig = config.getMessageStoreConfiguration();
    DBI               dbi             = new DBI(dbConfig.getUrl(), dbConfig.getUser(), dbConfig.getPassword()                     );
    DBI               messageDbi      = new DBI(messageDbConfig.getUrl(), messageDbConfig.getUser(), messageDbConfig.getPassword());

    dbi.registerArgumentFactory(new OptionalArgumentFactory(dbConfig.getDriverClass()));
    dbi.registerContainerFactory(new ImmutableListContainerFactory());
    dbi.registerContainerFactory(new ImmutableSetContainerFactory());
    dbi.registerContainerFactory(new OptionalContainerFactory());

    messageDbi.registerArgumentFactory(new OptionalArgumentFactory(dbConfig.getDriverClass()));
    messageDbi.registerContainerFactory(new ImmutableListContainerFactory());
    messageDbi.registerContainerFactory(new ImmutableSetContainerFactory());
    messageDbi.registerContainerFactory(new OptionalContainerFactory());

    Accounts accounts        = dbi.onDemand(Accounts.class       );
    Keys keys            = dbi.onDemand(Keys.class           );
    PendingAccounts pendingAccounts = dbi.onDemand(PendingAccounts.class);
    Messages messages        = messageDbi.onDemand(Messages.class       );

    logger.info("Vacuuming accounts...");
    accounts.vacuum();

    logger.info("Vacuuming pending_accounts...");
    pendingAccounts.vacuum();

    logger.info("Vacuuming keys...");
    keys.vacuum();

    logger.info("Vacuuming messages...");
    messages.vacuum();

    Thread.sleep(3000);
    System.exit(0);
  }
}
