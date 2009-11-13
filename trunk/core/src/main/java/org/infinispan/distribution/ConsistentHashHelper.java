package org.infinispan.distribution;

import org.infinispan.config.Configuration;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.Util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A helper class that handles the construction of consistent hash instances based on configuration.
 *
 * @author Manik Surtani
 * @since 4.0
 */
public class ConsistentHashHelper {

   /**
    * Returns a new consistent hash of the same type with the given address removed.
    *
    * @param ch       consistent hash to start with
    * @param toRemove address to remove
    * @param c        configuration
    * @return a new consistent hash instance of the same type
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash removeAddress(ConsistentHash ch, Address toRemove, Configuration c) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
      if (ch instanceof UnionConsistentHash)
         return removeAddressFromUnionConsistentHash((UnionConsistentHash) ch, toRemove, c);
      else {
         ConsistentHash newCH = (ConsistentHash) Util.getInstance(c.getConsistentHashClass());
         List<Address> caches = ch.getCaches();
         caches.remove(toRemove);
         newCH.setCaches(caches);
         return newCH;
      }
   }

   /**
    * Creates a new UnionConsistentHash instance based on the old instance, removing the provided address from both
    * target consistent hash instances in the union.
    *
    * @param uch union consistent hash instance
    * @param toRemove address to remove
    * @param c configuration
    * @return a new UnionConsistentHash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static UnionConsistentHash removeAddressFromUnionConsistentHash(UnionConsistentHash uch, Address toRemove, Configuration c) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
      ConsistentHash newFirstCH = removeAddress(uch.getOldConsistentHash(), toRemove, c);
      ConsistentHash newSecondCH = removeAddress(uch.getNewConsistentHash(), toRemove, c);
      return new UnionConsistentHash(newFirstCH, newSecondCH);
   }

   /**
    * Creates a new consistent hash instance based on the user's configuration, and populates the consistent hash
    * with the collection of addresses passed in.
    *
    * @param c configuration
    * @param addresses with which to populate the consistent hash
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Configuration c, List<Address> addresses) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
      ConsistentHash ch = (ConsistentHash) Util.getInstance(c.getConsistentHashClass());
      ch.setCaches(addresses);
      return ch;
   }

   /**
    * Creates a new consistent hash instance based on the user's configuration, and populates the consistent hash
    * with the collection of addresses passed in.
    *
    * @param c configuration
    * @param addresses with which to populate the consistent hash
    * @param moreAddresses to add to the list of addresses
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Configuration c, List<Address> addresses, Address... moreAddresses) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
      List<Address> list = new LinkedList<Address>(addresses);
      list.addAll(Arrays.asList(moreAddresses));
      return createConsistentHash(c, list);
   }

   /**
    * Creates a new consistent hash instance based on the user's configuration, and populates the consistent hash
    * with the collection of addresses passed in.
    *
    * @param c configuration
    * @param addresses with which to populate the consistent hash
    * @param moreAddresses to add to the list of addresses
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Configuration c, List<Address> addresses, Collection<Address> moreAddresses) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
      List<Address> list = new LinkedList<Address>(addresses);
      list.addAll(moreAddresses);
      return createConsistentHash(c, list);
   }

   /**
    * Creates a new consistent hash instance based on the type specified, and populates the consistent hash
    * with the collection of addresses passed in.
    *
    * @param clazz type of the consistent hash to create
    * @param addresses with which to populate the consistent hash
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Class<? extends ConsistentHash> clazz, List<Address> addresses) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
      ConsistentHash ch = Util.getInstance(clazz);
      if (addresses != null && addresses.size() > 0) ch.setCaches(addresses);
      return ch;
   }

   /**
    * Creates a new consistent hash instance based on the type specified, and populates the consistent hash
    * with the collection of addresses passed in.
    *
    * @param clazz type of the consistent hash to create
    * @param addresses with which to populate the consistent hash
    * @param moreAddresses to add to the list of addresses
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Class<? extends ConsistentHash> clazz, List<Address> addresses, Address... moreAddresses) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
      List<Address> list = new LinkedList<Address>(addresses);
      list.addAll(Arrays.asList(moreAddresses));
      return createConsistentHash(clazz, list);
   }

   /**
    * Creates a new consistent hash instance based on the type specified, and populates the consistent hash
    * with the collection of addresses passed in.
    * 
    * @param clazz type of the consistent hash to create
    * @param addresses with which to populate the consistent hash
    * @param moreAddresses to add to the list of addresses
    * @return a new consistent hash instance
    * @throws ClassNotFoundException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws IllegalAccessException thrown if the consistent hash type cannot be constructed via reflection.
    * @throws InstantiationException thrown if the consistent hash type cannot be constructed via reflection.
    */
   public static ConsistentHash createConsistentHash(Class<? extends ConsistentHash> clazz, List<Address> addresses, Collection<Address> moreAddresses) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
      List<Address> list = new LinkedList<Address>(addresses);
      list.addAll(moreAddresses);
      return createConsistentHash(clazz, list);
   }
}
