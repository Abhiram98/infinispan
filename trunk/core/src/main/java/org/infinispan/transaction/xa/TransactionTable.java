package org.infinispan.transaction.xa;

import org.infinispan.CacheException;
import org.infinispan.commands.CommandsFactory;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.config.Configuration;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.interceptors.InterceptorChain;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.transaction.Transaction;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository for {@link org.infinispan.transaction.xa.RemoteTransaction} and {@link
 * org.infinispan.transaction.xa.TransactionXaAdapter}s (locally originated trasactions).
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class TransactionTable {

   private static Log log = LogFactory.getLog(TransactionTable.class);
   private static boolean trace = log.isTraceEnabled();

   private Map<Transaction, TransactionXaAdapter> localTransactions = new HashMap<Transaction, TransactionXaAdapter>();

   private Map<GlobalTransaction, RemoteTransaction> remoteTransactions = new HashMap<GlobalTransaction, RemoteTransaction>();

   private CommandsFactory commandsFactory;
   private Configuration configuration;
   private InvocationContextContainer icc;
   private InterceptorChain invoker;
   private CacheNotifier notifier;
   private RpcManager rpcManager;
   private GlobalTransactionFactory gtf;


   @Inject
   public void initialize(CommandsFactory commandsFactory, RpcManager rpcManager, Configuration configuration,
                          InvocationContextContainer icc, InterceptorChain invoker, CacheNotifier notifier, GlobalTransactionFactory gtf) {
      this.commandsFactory = commandsFactory;
      this.rpcManager = rpcManager;
      this.configuration = configuration;
      this.icc = icc;
      this.invoker = invoker;
      this.notifier = notifier;
      this.gtf = gtf;
   }


   /**
    * Returns the {@link org.infinispan.transaction.xa.RemoteTransaction} associated with the supplied transaction id.
    * Returns null if no such association exists.
    */
   public RemoteTransaction getRemoteTransaction(GlobalTransaction txId) {
      return remoteTransactions.get(txId);
   }

   /**
    * Creates and register a {@link org.infinispan.transaction.xa.RemoteTransaction} based on the supplied params.
    * Returns the created transaction.
    *
    * @throws IllegalStateException if an attempt to create a {@link org.infinispan.transaction.xa.RemoteTransaction}
    *                               for an already registered id is made.
    */
   public RemoteTransaction createRemoteTransaction(GlobalTransaction globalTx, WriteCommand[] modifications) {
      RemoteTransaction remoteTransaction = new RemoteTransaction(modifications, globalTx);
      registerRemoteTransaction(globalTx, remoteTransaction);
      return remoteTransaction;
   }

   /**
    * Creates and register a {@link org.infinispan.transaction.xa.RemoteTransaction} with no modifications.
    * Returns the created transaction.
    *
    * @throws IllegalStateException if an attempt to create a {@link org.infinispan.transaction.xa.RemoteTransaction}
    *                               for an already registered id is made.
    */
   public RemoteTransaction createRemoteTransaction(GlobalTransaction globalTx) {
      RemoteTransaction remoteTransaction = new RemoteTransaction(globalTx);
      registerRemoteTransaction(globalTx, remoteTransaction);
      return remoteTransaction;
   }

   private void registerRemoteTransaction(GlobalTransaction gtx, RemoteTransaction rtx) {
      RemoteTransaction transaction = remoteTransactions.put(gtx, rtx);
      if (transaction != null) {
         String message = "A remote transaction with the given id was already registred!!!";
         log.error(message);
         throw new IllegalStateException(message);
      }

      if (trace) log.trace("Created and registered remote transaction " + rtx);
   }

   /**
    * Returns the {@link org.infinispan.transaction.xa.TransactionXaAdapter} corresponding to the supplied transaction.
    * If none exists, will be created first.
    */
   public TransactionXaAdapter getOrCreateXaAdapter(Transaction transaction, InvocationContext ctx) {
      TransactionXaAdapter current = localTransactions.get(transaction);
      if (current == null) {
         Address localAddress = rpcManager != null ? rpcManager.getTransport().getAddress() : null;
         GlobalTransaction tx = gtf.newGlobalTransaction(localAddress, false);
         if (trace) log.trace("Created a new GlobalTransaction {0}", tx);
         current = new TransactionXaAdapter(tx, icc, invoker, commandsFactory, configuration, this, transaction);
         localTransactions.put(transaction, current);
         try {
            transaction.enlistResource(current);
         } catch (Exception e) {
            log.error("Failed to emlist TransactionXaAdapter to transaction");
            throw new CacheException(e);
         }
         notifier.notifyTransactionRegistered(tx, ctx);
      }
      return current;
   }

   /**
    * Removes the {@link org.infinispan.transaction.xa.TransactionXaAdapter} coresponding to the given tx. Returns true
    * if such an tx exists.
    */
   public boolean removeLocalTransaction(Transaction tx) {
      return localTransactions.remove(tx) != null;
   }

   /**
    * Removes the {@link org.infinispan.transaction.xa.RemoteTransaction} coresponding to the given tx. Returns true if
    * such an tx exists.
    */
   public boolean removeRemoteTransaction(GlobalTransaction txId) {
      boolean existed = remoteTransactions.remove(txId) != null;
      if (trace) {
         log.trace("Removed " + txId + " from transaction table. Returning " + existed);
      }
      return existed;
   }

   public int getRemoteTxCount() {
      return remoteTransactions.size();
   }

   public int getLocalTxCount() {
      return localTransactions.size();
   }

   public TransactionXaAdapter getXaCacheAdapter(Transaction tx) {
      return localTransactions.get(tx);
   }

   public boolean containRemoteTx(GlobalTransaction globalTransaction) {
      return remoteTransactions.containsKey(globalTransaction);
   }
}
