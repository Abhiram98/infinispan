package org.infinispan.loaders.jdbc;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStore;
import org.infinispan.loaders.jdbc.mixed.JdbcMixedCacheStore;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStoreConfig;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.sql.Connection;

/**
 * Test to make sure that no two caches will use the same table for storing data.
 *
 * @author Mircea.Markus@jboss.com
 */
@Test(groups = "functional", testName = "loaders.jdbc.TableNameUniquenessTest")
public class TableNameUniquenessTest {

   public void testForJdbcStringBasedCacheStore() throws Exception {
      CacheManager cm = new DefaultCacheManager("configs/string-based.xml");
      Cache<String, String> first = cm.getCache("first");
      Cache<String, String> second = cm.getCache("second");

      CacheLoaderConfig firstCacheLoaderConfig = first.getConfiguration().getCacheLoaderManagerConfig().getFirstCacheLoaderConfig();
      assert firstCacheLoaderConfig != null;
      CacheLoaderConfig secondCacheLoaderConfig = second.getConfiguration().getCacheLoaderManagerConfig().getFirstCacheLoaderConfig();
      assert secondCacheLoaderConfig != null;
      assert firstCacheLoaderConfig instanceof JdbcStringBasedCacheStoreConfig;
      assert secondCacheLoaderConfig instanceof JdbcStringBasedCacheStoreConfig;

      JdbcStringBasedCacheStore firstCs = (JdbcStringBasedCacheStore) TestingUtil.extractComponent(first, CacheLoaderManager.class).getCacheLoader();
      JdbcStringBasedCacheStore secondCs = (JdbcStringBasedCacheStore) TestingUtil.extractComponent(second, CacheLoaderManager.class).getCacheLoader();

      asserTableExistance(firstCs.getConnectionFactory().getConnection(), "ISPN_STRING_TABLE_second", "ISPN_STRING_TABLE_first", "ISPN_STRING_TABLE");

      assertNoOverlapingState(first, second, firstCs, secondCs);
   }

   public void testForJdbcBinaryCacheStore() throws Exception {
      CacheManager cm = new DefaultCacheManager("configs/binary.xml");
      Cache<String, String> first = cm.getCache("first");
      Cache<String, String> second = cm.getCache("second");

      JdbcBinaryCacheStore firstCs = (JdbcBinaryCacheStore) TestingUtil.extractComponent(first, CacheLoaderManager.class).getCacheLoader();
      JdbcBinaryCacheStore secondCs = (JdbcBinaryCacheStore) TestingUtil.extractComponent(second, CacheLoaderManager.class).getCacheLoader();

      asserTableExistance(firstCs.getConnectionFactory().getConnection(), "ISPN_BUCKET_TABLE_second", "ISPN_BUCKET_TABLE_first", "IISPN_BUCKET_TABLE");

      assertNoOverlapingState(first, second, firstCs, secondCs);
   }

   @SuppressWarnings("unchecked")
   public void testForMixedCacheStore() throws Exception {
      CacheManager cm = new DefaultCacheManager("configs/mixed.xml");
      Cache first = cm.getCache("first");
      Cache second = cm.getCache("second");

      JdbcMixedCacheStore firstCs = (JdbcMixedCacheStore) TestingUtil.extractComponent(first, CacheLoaderManager.class).getCacheLoader();
      JdbcMixedCacheStore secondCs = (JdbcMixedCacheStore) TestingUtil.extractComponent(second, CacheLoaderManager.class).getCacheLoader();

      asserTableExistance(firstCs.getConnectionFactory().getConnection(), "ISPN_MIXED_STR_TABLE_second", "ISPN_MIXED_STR_TABLE_first", "ISPN_MIXED_STR_TABLE");
      asserTableExistance(firstCs.getConnectionFactory().getConnection(), "ISPN_MIXED_BINARY_TABLE_second", "ISPN_MIXED_BINARY_TABLE_first", "ISPN_MIXED_BINARY_TABLE");

      assertNoOverlapingState(first, second, firstCs, secondCs);


      Person person1 = new Person(29, "Mircea");
      Person person2 = new Person(29, "Manik");

      first.put("k", person1);
      assert firstCs.containsKey("k");
      assert !secondCs.containsKey("k");
      assert first.get("k").equals(person1);
      assert second.get("k") == null;

      second.put("k2", person2);
      assert second.get("k2").equals(person2);
      assert first.get("k2") == null;
   }


   static class Person implements Serializable {
      int age;
      String name;

      Person(int age, String name) {
         this.age = age;
         this.name = name;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (!(o instanceof Person)) return false;

         Person person = (Person) o;

         if (age != person.age) return false;
         if (name != null ? !name.equals(person.name) : person.name != null) return false;

         return true;
      }

      @Override
      public int hashCode() {
         int result = age;
         result = 31 * result + (name != null ? name.hashCode() : 0);
         return result;
      }
   }

   private void asserTableExistance(Connection connection, String secondTable, String firstTable, String tablePrefix) throws Exception {
      assert !TableManipulationTest.existsTable(connection, tablePrefix) : "this table should not exist!";
      assert TableManipulationTest.existsTable(connection, firstTable);
      assert TableManipulationTest.existsTable(connection, secondTable);
      connection.close();
   }

   private void assertNoOverlapingState(Cache<String, String> first, Cache<String, String> second, CacheStore firstCs, CacheStore secondCs) throws CacheLoaderException {
      first.put("k", "v");
      assert firstCs.containsKey("k");
      assert !secondCs.containsKey("k");
      assert first.get("k").equals("v");
      assert second.get("k") == null;

      second.put("k2", "v2");
      assert second.get("k2").equals("v2");
      assert first.get("k2") == null;
   }
}
