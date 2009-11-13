package org.infinispan.loaders.jdbc.stringbased;

import org.infinispan.loaders.LockSupportCacheStoreConfig;
import org.infinispan.loaders.jdbc.TableManipulation;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactoryConfig;
import org.infinispan.util.Util;

/**
 * Configuration for {@link org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore} cache store.
 *
 * @author Mircea.Markus@jboss.com
 * @see org.infinispan.loaders.jdbc.stringbased.Key2StringMapper
 */
public class JdbcStringBasedCacheStoreConfig extends LockSupportCacheStoreConfig {

   private Key2StringMapper key2StringMapper;

   private ConnectionFactoryConfig connectionFactoryConfig = new ConnectionFactoryConfig();
   private TableManipulation tableManipulation = new TableManipulation();
   private boolean manageConnectionFactory = true;

   public JdbcStringBasedCacheStoreConfig(ConnectionFactoryConfig connectionFactoryConfig, TableManipulation tableManipulation) {
      this();
      this.connectionFactoryConfig = connectionFactoryConfig;
      this.tableManipulation = tableManipulation;
   }

   public JdbcStringBasedCacheStoreConfig() {
      cacheLoaderClassName = JdbcStringBasedCacheStore.class.getName();
   }

   public JdbcStringBasedCacheStoreConfig(boolean manageConnectionFactory) {
      this();
      this.manageConnectionFactory = manageConnectionFactory;
   }

   public Key2StringMapper getKey2StringMapper() {
      if (key2StringMapper == null) {
         try {
            key2StringMapper = DefaultKey2StringMapper.class.newInstance();
         } catch (Exception e) {
            throw new IllegalStateException("This should never happen", e);
         }
      }
      return key2StringMapper;
   }

   /**
    * Name of the class implementing Key2StringMapper. The default value is {@link org.infinispan.loaders.jdbc.stringbased.DefaultKey2StringMapper}
    *
    * @see org.infinispan.loaders.jdbc.stringbased.Key2StringMapper
    */
   public void setKey2StringMapperClass(String className) {
      testImmutability("key2StringMapper");
      try {
         key2StringMapper = (Key2StringMapper) Util.getInstance(className);
      } catch (Exception e) {
         throw new IllegalArgumentException("Could not load Key2StringMapper :'" + className + "'", e);
      }
   }

   /**
    * Sets the prefix for the name of the table where the data will be stored. "_<cache name>" will be appended
    * to this prefix in order to enforce unique table names for each cache.
    */
   public void setStringsTableNamePrefix(String stringsTableNamePrefix) {
      testImmutability("tableManipulation");
      this.tableManipulation.setTableNamePrefix(stringsTableNamePrefix);
   }

   /**
    * Sets the name of the table where data will be stored.
    */
   public void setCacheName(String cacheName) {
      testImmutability("tableManipulation");
      this.tableManipulation.setCacheName(cacheName);
   }

   /**
    * Sets the name of the column where the id will be stored. The id is obtained through:
    * <pre>
    *   key2StringMapper.getStringMapping(storedEntry.getKey());
    * </pre>
    * Mandatory.
    */
   public void setIdColumnName(String idColumnName) {
      testImmutability("tableManipulation");
      this.tableManipulation.setIdColumnName(idColumnName);
   }

   /**
    * Sets the name of the column where the StoredEntry will be binary stored. Mandatory.
    */
   public void setDataColumnName(String dataColumnName) {
      testImmutability("tableManipulation");
      this.tableManipulation.setDataColumnName(dataColumnName);
   }

   /**
    * Sets the name of the column where the timestamp (Long in java) will be stored. Mandatory.
    */
   public void setTimestampColumnName(String timestampColumnName) {
      testImmutability("tableManipulation");
      this.tableManipulation.setTimestampColumnName(timestampColumnName);
   }

   /**
    * Sets the prefix for the name of the table where the data will be stored. "_<cache name>" will be appended
    * to this prefix in order to enforce unique table names for each cache.
    */
   public void setTimestampColumnType(String timestampColumnType) {
      testImmutability("tableManipulation");
      this.tableManipulation.setTimestampColumnType(timestampColumnType);
   }

   public void setConnectionFactoryClass(String connectionFactoryClass) {
      testImmutability("connectionFactoryConfig");
      this.connectionFactoryConfig.setConnectionFactoryClass(connectionFactoryClass);
   }

   public ConnectionFactoryConfig getConnectionFactoryConfig() {
      return connectionFactoryConfig;
   }

   public TableManipulation getTableManipulation() {
      return tableManipulation;
   }

   /**
    * Jdbc connection string for connecting to the database. Mandatory.
    */
   public void setConnectionUrl(String connectionUrl) {
      testImmutability("connectionFactoryConfig");
      this.connectionFactoryConfig.setConnectionUrl(connectionUrl);
   }

   /**
    * Database username.
    */
   public void setUserName(String userName) {
      testImmutability("connectionFactoryConfig");
      this.connectionFactoryConfig.setUserName(userName);
   }

   public void setDatasourceJndiLocation(String location) {
      testImmutability("datasourceJndiLocation");
      this.connectionFactoryConfig.setDatasourceJndiLocation(location);
   }

   /**
    * Database username's password.
    */
   public void setPassword(String password) {
      testImmutability("connectionFactoryConfig");
      this.connectionFactoryConfig.setPassword(password);
   }

   /**
    * The name of the driver used for connecting to the database. Mandatory, will be loaded before initiating the first
    * connection.
    */
   public void setDriverClass(String driverClassName) {
      testImmutability("connectionFactoryConfig");
      this.connectionFactoryConfig.setDriverClass(driverClassName);
   }

   /**
    * sql equivalent for java's String. Mandatory.
    */
   public void setIdColumnType(String idColumnType) {
      testImmutability("tableManipulation");
      this.tableManipulation.setIdColumnType(idColumnType);
   }

   /**
    * Sets the type of the column where data will be binary stored. BLOB-like type, DBMS dependent. Mandatory.
    */
   public void setDataColumnType(String dataColumnType) {
      testImmutability("tableManipulation");
      this.tableManipulation.setDataColumnType(dataColumnType);
   }

   public void setDropTableOnExit(boolean dropTableOnExit) {
      testImmutability("tableManipulation");
      this.tableManipulation.setDropTableOnExit(dropTableOnExit);
   }

   public void setCreateTableOnStart(boolean createTableOnStart) {
      testImmutability("tableManipulation");
      this.tableManipulation.setCreateTableOnStart(createTableOnStart);
   }

   /**
    * If this method returns false, then the connection factory should not be created by the {@link
    * org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore}, but will be injected through {@link
    * org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore#doConnectionFactoryInitialization(org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactory)}
    */
   boolean isManageConnectionFactory() {
      return manageConnectionFactory;
   }

   public void setTableManipulation(TableManipulation tableManipulation) {
      testImmutability("tableManipulation");
      this.tableManipulation = tableManipulation;
   }

   /**
    * @see org.infinispan.loaders.jdbc.TableManipulation#getFetchSize()
    */
   public void setFetchSize(int fetchSize) {
      testImmutability("tableManipulation");
      this.tableManipulation.setFetchSize(fetchSize);
   }

   /**
    * @see org.infinispan.loaders.jdbc.TableManipulation#getBatchSize()
    */
   public void setBatchSize(int batchSize) {
      testImmutability("tableManipulation");
      this.tableManipulation.setBatchSize(batchSize);
   }

   /**
    * @see org.infinispan.loaders.jdbc.TableManipulation#getFetchSize()
    */
   public int getFetchSize() {
      return this.tableManipulation.getFetchSize();
   }

   /**
    * @see org.infinispan.loaders.jdbc.TableManipulation#getBatchSize()
    */
   public int getBatchSize() {
      return this.tableManipulation.getBatchSize();
   }

   @Override
   public JdbcStringBasedCacheStoreConfig clone() {
      JdbcStringBasedCacheStoreConfig result = (JdbcStringBasedCacheStoreConfig) super.clone();
      result.connectionFactoryConfig = connectionFactoryConfig.clone();
      result.tableManipulation = tableManipulation.clone();
      return result;
   }   
}
