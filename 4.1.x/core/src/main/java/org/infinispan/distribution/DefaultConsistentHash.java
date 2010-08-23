package org.infinispan.distribution;

import org.infinispan.marshall.Ids;
import org.infinispan.marshall.Marshallable;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.hash.MurmurHash2;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.Math.min;
import static org.infinispan.util.hash.MurmurHash2.hash;

@Marshallable(externalizer = DefaultConsistentHash.Externalizer.class, id = Ids.DEFAULT_CONSISTENT_HASH)
public class DefaultConsistentHash extends AbstractConsistentHash {

   // make sure all threads see the current list
   ArrayList<Address> addresses;
   SortedMap<Integer, Address> positions;
   // TODO: Maybe address and addressToHashIds can be combined in a LinkedHashMap?
   Map<Address, Integer> addressToHashIds;

   final static int HASH_SPACE = 10240; // no more than 10k nodes?

   public void setCaches(List<Address> caches) {

      addresses = new ArrayList<Address>(caches);

      // this list won't grow.
      addresses.trimToSize();

      positions = new TreeMap<Integer, Address>();
      addressToHashIds = new HashMap<Address, Integer>();

      for (Address a : addresses) {
         int positionIndex = Math.abs(hash(a)) % HASH_SPACE;
         // this is deterministic since the address list is ordered and the order is consistent across the grid
         while (positions.containsKey(positionIndex)) positionIndex = positionIndex + 1 % HASH_SPACE;
         positions.put(positionIndex, a);
         // If address appears several times, take the lowest value to guarantee that
         // at least the initial value and subsequent +1 values would end up in the same node
         // TODO: Remove this check since https://jira.jboss.org/jira/browse/ISPN-428 contains a proper fix for this
         if (!addressToHashIds.containsKey(a))
            addressToHashIds.put(a, positionIndex);
      }

      addresses.clear();
      // reorder addresses as per the positions.
      for (Address a : positions.values()) addresses.add(a);
   }

   public List<Address> getCaches() {
      return addresses;
   }

   public List<Address> locate(Object key, int replCount) {
      int keyHashCode = hash(key);
      if (keyHashCode == Integer.MIN_VALUE) keyHashCode += 1;
      int hash = Math.abs(keyHashCode);
      int numCopiesToFind = min(replCount, addresses.size());

      List<Address> owners = new ArrayList<Address>(numCopiesToFind);

      SortedMap<Integer, Address> candidates = positions.tailMap(hash % HASH_SPACE);

      int numOwnersFound = 0;

      for (Address a : candidates.values()) {
         if (numOwnersFound < numCopiesToFind) {
            owners.add(a);
            numOwnersFound++;
         } else {
            break;
         }
      }

      if (numOwnersFound < numCopiesToFind) {
         for (Address a : positions.values()) {
            if (numOwnersFound < numCopiesToFind) {
               owners.add(a);
               numOwnersFound++;
            } else {
               break;
            }
         }
      }

      return owners;
   }

   @Override
   public boolean isKeyLocalToAddress(Address target, Object key, int replCount) {
      // more efficient impl
      int keyHashCode = hash(key);
      if (keyHashCode == Integer.MIN_VALUE) keyHashCode += 1;
      int hash = Math.abs(keyHashCode);
      int numCopiesToFind = min(replCount, addresses.size());

      SortedMap<Integer, Address> candidates = positions.tailMap(hash % HASH_SPACE);
      int nodesTested = 0;
      for (Address a : candidates.values()) {
         if (nodesTested < numCopiesToFind) {
            if (a.equals(target)) return true;
            nodesTested++;
         } else {
            break;
         }
      }

      // start from the beginning
      if (nodesTested < numCopiesToFind) {
         for (Address a : positions.values()) {
            if (nodesTested < numCopiesToFind) {
               if (a.equals(target)) return true;
               nodesTested++;
            } else {
               break;
            }
         }
      }

      return false;
   }


   public int getDistance(Address a1, Address a2) {
      if (a1 == null || a2 == null) throw new NullPointerException("Cannot deal with nulls as parameters!");

      int p1 = addresses.indexOf(a1);
      if (p1 < 0)
         return -1;

      int p2 = addresses.indexOf(a2);
      if (p2 < 0)
         return -1;

      if (p1 <= p2)
         return p2 - p1;
      else
         return addresses.size() - (p1 - p2);
   }

   public boolean isAdjacent(Address a1, Address a2) {
      int distance = getDistance(a1, a2);
      return distance == 1 || distance == addresses.size() - 1;
   }

   @Override
   public int getHashId(Address a) {
      Integer hashId = addressToHashIds.get(a);
      if (hashId == null)
         return -1;
      else
         return hashId.intValue();
   }

   @Override
   public int getHashSpace() {
      return HASH_SPACE;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      DefaultConsistentHash that = (DefaultConsistentHash) o;

      if (addresses != null ? !addresses.equals(that.addresses) : that.addresses != null) return false;
      if (positions != null ? !positions.equals(that.positions) : that.positions != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      int result = addresses != null ? addresses.hashCode() : 0;
      result = 31 * result + (positions != null ? positions.hashCode() : 0);
      return result;
   }

   public static class Externalizer implements org.infinispan.marshall.Externalizer {
      public void writeObject(ObjectOutput output, Object subject) throws IOException {
         DefaultConsistentHash dch = (DefaultConsistentHash) subject;
         output.writeObject(dch.addresses);
         output.writeObject(dch.positions);
         output.writeObject(dch.addressToHashIds);
      }

      @SuppressWarnings("unchecked")
      public Object readObject(ObjectInput unmarshaller) throws IOException, ClassNotFoundException {
         DefaultConsistentHash dch = new DefaultConsistentHash();
         dch.addresses = (ArrayList<Address>) unmarshaller.readObject();
         dch.positions = (SortedMap<Integer, Address>) unmarshaller.readObject();
         dch.addressToHashIds = (Map<Address, Integer>) unmarshaller.readObject();
         return dch;
      }
   }

   @Override
   public String toString() {
      return "DefaultConsistentHash{" +
              "addresses =" + positions +
              ", hash space =" + HASH_SPACE +
              '}';
   }
}