package com.opennetwork.secureim.server.workers;

import com.opennetwork.secureim.server.util.Util;
import com.opennetwork.secureim.server.storage.Account;
import com.opennetwork.secureim.server.storage.DirectoryManager;
import com.opennetwork.secureim.server.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.opennetwork.secureim.server.entities.ClientContact;
import com.opennetwork.secureim.server.storage.Account;
import com.opennetwork.secureim.server.storage.AccountsManager;
import com.opennetwork.secureim.server.storage.DirectoryManager;
import com.opennetwork.secureim.server.storage.DirectoryManager.BatchOperationHandle;

import java.util.List;

public class DirectoryUpdater {

  private static final int CHUNK_SIZE = 10000;

  private final Logger logger = LoggerFactory.getLogger(DirectoryUpdater.class);

  private final AccountsManager        accountsManager;
  private final DirectoryManager directory;

  public DirectoryUpdater(AccountsManager accountsManager, DirectoryManager directory)
  {
    this.accountsManager        = accountsManager;
    this.directory              = directory;
  }

  public void updateFromLocalDatabase() {
    int                  contactsAdded   = 0;
    int                  contactsRemoved = 0;
    DirectoryManager.BatchOperationHandle batchOperation  = directory.startBatchOperation();

    try {
      logger.info("Updating from local DB.");
      int offset = 0;

      for (;;) {
        List<Account> accounts = accountsManager.getAll(offset, CHUNK_SIZE);

        if (accounts == null || accounts.isEmpty()) break;
        else                                        offset += accounts.size();

        for (Account account : accounts) {
          if (account.isActive()) {
            byte[]        token         = Util.getContactToken(account.getNumber());
            ClientContact clientContact = new ClientContact(token, null, account.isVoiceSupported(), account.isVideoSupported());

            directory.add(batchOperation, clientContact);
            contactsAdded++;
          } else {
            directory.remove(batchOperation, account.getNumber());
            contactsRemoved++;
          }
        }

        logger.info("Processed " + CHUNK_SIZE + " local accounts...");
      }
    } finally {
      directory.stopBatchOperation(batchOperation);
    }

    logger.info(String.format("Local directory is updated (%d added, %d removed).", contactsAdded, contactsRemoved));
  }

//  public void updateFromPeers() {
//    logger.info("Updating peer directories.");
//
//    int                   contactsAdded   = 0;
//    int                   contactsRemoved = 0;
//    List<FederatedClient> clients         = federatedClientManager.getClients();
//
//    for (FederatedClient client : clients) {
//      logger.info("Updating directory from peer: " + client.getPeerName());
//
//      int userCount = client.getUserCount();
//      int retrieved = 0;
//
//      logger.info("Remote peer user count: " + userCount);
//
//      while (retrieved < userCount) {
//        logger.info("Retrieving remote tokens...");
//        List<ClientContact>        remoteContacts = client.getUserTokens(retrieved);
//        List<PendingClientContact> localContacts  = new LinkedList<>();
//        BatchOperationHandle       handle         = directory.startBatchOperation();
//
//        if (remoteContacts == null) {
//          logger.info("Remote tokens empty, ending...");
//          break;
//        } else {
//          logger.info("Retrieved " + remoteContacts.size() + " remote tokens...");
//        }
//
//        for (ClientContact remoteContact : remoteContacts) {
//          localContacts.add(directory.get(handle, remoteContact.getToken()));
//        }
//
//        directory.stopBatchOperation(handle);
//
//        handle = directory.startBatchOperation();
//        Iterator<ClientContact>        remoteContactIterator = remoteContacts.iterator();
//        Iterator<PendingClientContact> localContactIterator  = localContacts.iterator();
//
//        while (remoteContactIterator.hasNext() && localContactIterator.hasNext()) {
//          try {
//            ClientContact           remoteContact = remoteContactIterator.next();
//            Optional<ClientContact> localContact  = localContactIterator.next().get();
//
//            remoteContact.setRelay(client.getPeerName());
//
//            if (!remoteContact.isInactive() && (!localContact.isPresent() || client.getPeerName().equals(localContact.get().getRelay()))) {
//              contactsAdded++;
//              directory.add(handle, remoteContact);
//            } else {
//              if (localContact.isPresent() && client.getPeerName().equals(localContact.get().getRelay())) {
//                contactsRemoved++;
//                directory.remove(handle, remoteContact.getToken());
//              }
//            }
//          } catch (IOException e) {
//            logger.warn("JSON Serialization Failed: ", e);
//          }
//        }
//
//        directory.stopBatchOperation(handle);
//
//        retrieved += remoteContacts.size();
//        logger.info("Processed: " + retrieved + " remote tokens.");
//      }
//
//      logger.info("Update from peer complete.");
//    }
//
//    logger.info("Update from peer directories complete.");
//    logger.info(String.format("Added %d and removed %d remove contacts.", contactsAdded, contactsRemoved));
//  }
}
