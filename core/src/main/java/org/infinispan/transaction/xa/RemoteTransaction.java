package org.infinispan.transaction.xa;

import org.infinispan.commands.write.WriteCommand;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.util.BidirectionalLinkedHashMap;
import org.infinispan.util.BidirectionalMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Defines the state of a remotely originated transaction.
 *
 * @author Mircea.Markus@jboss.com
 * @since 4.0
 */
public class RemoteTransaction implements CacheTransaction, Cloneable {

   private List<WriteCommand> modifications;

   private BidirectionalLinkedHashMap<Object, CacheEntry> lookedUpEntries;

   private GlobalTransaction tx;


   public RemoteTransaction(WriteCommand[] modifications, GlobalTransaction tx) {
      this.modifications = Arrays.asList(modifications);
      lookedUpEntries = new BidirectionalLinkedHashMap<Object, CacheEntry>(modifications.length);
      this.tx = tx;
   }

   public RemoteTransaction(GlobalTransaction tx) {
      this.modifications = new LinkedList<WriteCommand>();
      lookedUpEntries = new BidirectionalLinkedHashMap<Object, CacheEntry>();
      this.tx = tx;
   }

   public GlobalTransaction getGlobalTransaction() {
      return tx;
   }

   public List<WriteCommand> getModifications() {
      return modifications;
   }
   
   public void setModifications(WriteCommand[] modifications){
      this.modifications = Arrays.asList(modifications);  
   }

   public CacheEntry lookupEntry(Object key) {
      return lookedUpEntries.get(key);
   }

   public BidirectionalMap<Object, CacheEntry> getLookedUpEntries() {
      return lookedUpEntries;
   }

   public void putLookedUpEntry(Object key, CacheEntry e) {
      lookedUpEntries.put(key, e);
   }

   public void removeLookedUpEntry(Object key) {
      lookedUpEntries.remove(key);
   }

   public void clearLookedUpEntries() {
      lookedUpEntries.clear();
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof RemoteTransaction)) return false;
      RemoteTransaction that = (RemoteTransaction) o;
      return tx.equals(that.tx);
   }

   @Override
   public int hashCode() {
      return tx.hashCode();
   }

   @Override
   @SuppressWarnings("unchecked")
   public Object clone() {
      try {
         RemoteTransaction dolly = (RemoteTransaction) super.clone();
         dolly.modifications = new ArrayList<WriteCommand>(modifications);
         dolly.lookedUpEntries = lookedUpEntries.clone();
         return dolly;
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException("Impossible!!");
      }
   }

   @Override
   public String toString() {
      return "RemoteTransaction{" +
            "modifications=" + modifications +
            ", lookedUpEntries=" + lookedUpEntries +
            ", tx=" + tx +
            '}';
   }
}
