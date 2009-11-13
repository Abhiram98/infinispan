package org.infinispan.distribution.rehash;

import org.infinispan.Cache;
import org.infinispan.distribution.BaseDistFunctionalTest;
import org.infinispan.distribution.ConsistentHash;
import org.infinispan.distribution.ConsistentHashHelper;
import org.infinispan.manager.CacheManager;
import org.infinispan.remoting.transport.Address;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests performing some work on the joiner during a JOIN
 *
 * @author Manik Surtani
 * @since 4.0
 */
@Test(groups = "functional", testName = "distribution.rehash.WorkDuringJoinTest")
public class WorkDuringJoinTest extends BaseDistFunctionalTest {

   CacheManager joinerManager;
   Cache<Object, String> joiner;

   public WorkDuringJoinTest() {
      INIT_CLUSTER_SIZE = 2;
   }

   private List<MagicKey> init() {
      List<MagicKey> keys = new ArrayList<MagicKey>(Arrays.asList(
            new MagicKey(c1, "k1"), new MagicKey(c2, "k2"),
            new MagicKey(c1, "k3"), new MagicKey(c2, "k4")
      ));

      int i = 0;
      for (Cache<Object, String> c : caches) c.put(keys.get(i++), "v" + i);

      log.info("Initialized with keys {0}", keys);
      return keys;
   }

   Address startNewMember() {
      joinerManager = addClusterEnabledCacheManager();
      joinerManager.defineConfiguration(cacheName, configuration);
      joiner = joinerManager.getCache(cacheName);
      return joiner.getCacheManager().getAddress();
   }

   public void testJoinAndGet() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
      List<MagicKey> keys = init();
      ConsistentHash chOld = getConsistentHash(c1);
      Address joinerAddress = startNewMember();
      ConsistentHash chNew = ConsistentHashHelper.createConsistentHash(chOld.getClass(), chOld.getCaches(), joinerAddress);
      // which key should me mapped to the joiner?
      MagicKey keyToTest = null;
      for (MagicKey k: keys) {
         if (chNew.isKeyLocalToAddress(joinerAddress, k, NUM_OWNERS)) {
            keyToTest = k;
            break;
         }
      }

      if (keyToTest == null) throw new NullPointerException("Couldn't find a key mapped to J!");
      assert joiner.get(keyToTest) != null;
   }
}
