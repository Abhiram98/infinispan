package org.infinispan.distribution;

import org.infinispan.CacheException;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.control.RehashControlCommand;
import static org.infinispan.commands.control.RehashControlCommand.Type.*;
import org.infinispan.config.Configuration;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheValue;
import static org.infinispan.distribution.ConsistentHashHelper.createConsistentHash;
import org.infinispan.remoting.responses.Response;
import org.infinispan.remoting.responses.SuccessfulResponse;
import static org.infinispan.remoting.rpc.ResponseMode.SYNCHRONOUS;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.Util;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 5.  JoinTask: This is a PULL based rehash.  JoinTask is kicked off on the JOINER. 5.1.  Obtain OLD_CH from
 * coordinator (using GetConsistentHashCommand) 5.2.  Generate TEMP_CH (which is a union of OLD_CH and NEW_CH) 5.3.
 * Broadcast TEMP_CH across the cluster (using InstallConsistentHashCommand) 5.4.  Log all incoming writes/txs and
 * respond with a positive ack. 5.5.  Ignore incoming reads, forcing callers to check next owner of data. 5.6.  Ping
 * each node in OLD_CH's view and ask for state (PullStateCommand) 5.7.  Apply state received from 5.6. 5.8.  Drain tx
 * log and apply, stop logging writes once drained. 5.9.  Reverse 5.5. 5.10. Broadcast NEW_CH so this is applied (using
 * InstallConsistentHashCommand) 5.11. Loop through data container and unicast invalidations for keys that "could" exist
 * on OLD_CH and not in NEW_CH
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class JoinTask extends RehashTask {

   private static final Log log = LogFactory.getLog(JoinTask.class);
   ConsistentHash chOld;
   ConsistentHash chNew;
   Address self;

   public JoinTask(RpcManager rpcManager, CommandsFactory commandsFactory, Configuration conf,
                   TransactionLogger transactionLogger, DataContainer dataContainer, DistributionManagerImpl dmi) {
      super(dmi, rpcManager, conf, transactionLogger, commandsFactory, dataContainer);
      this.dataContainer = dataContainer;
      self = rpcManager.getTransport().getAddress();
   }

   @SuppressWarnings("unchecked")
   private List<Address> parseResponses(List<Response> resp) {
      for (Response r : resp) {
         if (r instanceof SuccessfulResponse) {
            return (List<Address>) ((SuccessfulResponse) r).getResponseValue();
         }
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   private Map<Object, InternalCacheValue> getStateFromResponse(SuccessfulResponse r) {
      return (Map<Object, InternalCacheValue>) r.getResponseValue();
   }

   protected void performRehash() throws Exception {
      long start = System.currentTimeMillis();
      if (log.isDebugEnabled()) log.debug("Commencing");
      boolean unlocked = false;
      try {
         dmi.joinComplete = false;
         // 1.  Get chOld from coord.
         // this happens in a loop to ensure we receive the correct CH and not a "union".
         // TODO make at least *some* of these configurable!
         long minSleepTime = 500, maxSleepTime = 2000; // sleep time between retries
         int maxWaitTime = (int) configuration.getRehashRpcTimeout() * 10; // after which we give up!
         Random rand = new Random();
         long giveupTime = System.currentTimeMillis() + maxWaitTime;
         do {
            if (log.isTraceEnabled()) log.trace("Requesting old consistent hash from coordinator");
            List<Response> resp = rpcManager.invokeRemotely(coordinator(),
                                                            cf.buildRehashControlCommand(JOIN_REQ, self),
                                                            SYNCHRONOUS, configuration.getRehashRpcTimeout(), true);
            List<Address> addresses = parseResponses(resp);

            if (log.isDebugEnabled()) log.debug("Retrieved old consistent hash address list {0}", addresses);
            if (addresses == null) {
               long time = rand.nextInt((int) (maxSleepTime - minSleepTime) / 10);
               time = (time * 10) + minSleepTime;
               if (log.isTraceEnabled()) log.trace("Sleeping for {0}", Util.prettyPrintTime(time));
               Thread.sleep(time); // sleep for a while and retry
            } else {
               chOld = createConsistentHash(configuration, addresses);
            }
         } while (chOld == null && System.currentTimeMillis() < giveupTime);

         if (chOld == null)
            throw new CacheException("Unable to retrieve old consistent hash from coordinator even after several attempts at sleeping and retrying!");

         // 2.  new CH instance
         chNew = createConsistentHash(configuration, chOld.getCaches(), self);
         dmi.setConsistentHash(chNew);
         
         if (configuration.isRehashEnabled()) {
            // 3.  Enable TX logging
            transactionLogger.enable();

            // 4.  Broadcast new temp CH
            rpcManager.broadcastRpcCommand(cf.buildRehashControlCommand(JOIN_REHASH_START, self), true, true);

            // 5.  txLogger being enabled will cause ClusteredGetCommands to return uncertain responses.

            // 6.  pull state from everyone.
            Address myAddress = rpcManager.getTransport().getAddress();
            RehashControlCommand cmd = cf.buildRehashControlCommand(PULL_STATE, myAddress, null, chNew);
            // TODO I should be able to process state chunks from different nodes simultaneously!!
            List<Address> addressesWhoMaySendStuff = getAddressesWhoMaySendStuff(configuration.getNumOwners());
            List<Response> resps = rpcManager.invokeRemotely(addressesWhoMaySendStuff, cmd, SYNCHRONOUS, configuration.getRehashRpcTimeout(), true);

            // 7.  Apply state
            for (Response r : resps) {
               if (r instanceof SuccessfulResponse) {
                  Map<Object, InternalCacheValue> state = getStateFromResponse((SuccessfulResponse) r);
                  dmi.applyState(chNew, state);
               }
            }

            // 8.  Drain logs
            dmi.drainLocalTransactionLog();
         }
         unlocked = true;

         if (!configuration.isRehashEnabled()) {
            rpcManager.broadcastRpcCommand(cf.buildRehashControlCommand(JOIN_REHASH_START, self), true, true);            
         }
         // 10.
         rpcManager.broadcastRpcCommand(cf.buildRehashControlCommand(JOIN_REHASH_END, self), true, true);
         rpcManager.invokeRemotely(coordinator(), cf.buildRehashControlCommand(JOIN_COMPLETE, self), SYNCHRONOUS,
                                   configuration.getRehashRpcTimeout(), true);

         if (configuration.isRehashEnabled()) {
            // 11.
            invalidateInvalidHolders(chOld, chNew);
         }

         if (log.isInfoEnabled())
            log.info("Completed in {0}!", Util.prettyPrintTime(System.currentTimeMillis() - start));
      } catch (Exception e) {
         log.error("Caught exception!", e);
         throw new CacheException("Unexpected exception", e);
      } finally {
         if (!unlocked) transactionLogger.unlockAndDisable();
         dmi.joinComplete = true;
      }
   }

   // TODO unit test this!!!
   List<Address> getAddressesWhoMaySendStuff(int replCount) {
      List<Address> l = new LinkedList<Address>();
      List<Address> caches = chNew.getCaches();
      int selfIdx = caches.indexOf(self);
      if (selfIdx >= replCount - 1) {
         l.addAll(caches.subList(selfIdx - replCount + 1, selfIdx));
      } else {
         l.addAll(caches.subList(0, selfIdx));
         int alreadyCollected = l.size();
         l.addAll(caches.subList(caches.size() - replCount + 1 + alreadyCollected, caches.size()));
      }

      Address plusOne;
      if (selfIdx == caches.size() - 1)
         plusOne = caches.get(0);
      else
         plusOne = caches.get(selfIdx + 1);

      if (!l.contains(plusOne)) l.add(plusOne);
      return l;
   }
}
