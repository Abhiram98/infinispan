package org.infinispan.loaders.jdbc;

import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 * Contains all the logic of manipulating the table, including creating it if needed and access operations like
 * inserting, selecting etc. Used by JDBC based cache loaders.
 *
 * @author Mircea.Markus@jboss.com
 */
public class TableManipulation implements Cloneable {

   private static Log log = LogFactory.getLog(TableManipulation.class);

   public static final int DEFAULT_FETCH_SIZE = 100;

   public static final int DEFAULT_BATCH_SIZE = 100;

   private String idColumnName;
   private String idColumnType;
   private String tableName;
   private String tableNamePrefix;
   private String cacheName;
   private String dataColumnName;
   private String dataColumnType;
   private String timestampColumnName;
   private String timestampColumnType;
   private int fetchSize = DEFAULT_FETCH_SIZE;
   private int batchSize = DEFAULT_BATCH_SIZE;

   /*
   * following two params manage creation and destruction during start up/shutdown.
   */
   boolean createTableOnStart = true;
   boolean dropTableOnExit = false;
   private ConnectionFactory connectionFactory;

   /* Cache the sql for managing data */
   private String insertRowSql;
   private String updateRowSql;
   private String selectRowSql;
   private String deleteRowSql;
   private String loadAllRowsSql;
   private String deleteAllRows;
   private String selectExpiredRowsSql;
   private String deleteExpiredRowsSql;

   public TableManipulation(String idColumnName, String idColumnType, String tableNamePrefix, String dataColumnName,
                            String dataColumnType, String timestampColumnName, String timestampColumnType) {
      this.idColumnName = idColumnName;
      this.idColumnType = idColumnType;
      this.tableNamePrefix = tableNamePrefix;
      this.dataColumnName = dataColumnName;
      this.dataColumnType = dataColumnType;
      this.timestampColumnName = timestampColumnName;
      this.timestampColumnType = timestampColumnType;
   }

   public TableManipulation() {
   }

   public boolean tableExists(Connection connection, String tableName) throws CacheLoaderException {
      assrtNotNull(getTableName(), "table name is mandatory");
      ResultSet rs = null;
      try {
         // (a j2ee spec compatible jdbc driver has to fully
         // implement the DatabaseMetaData)
         DatabaseMetaData dmd = connection.getMetaData();
         String catalog = connection.getCatalog();
         String schema = null;
         String quote = dmd.getIdentifierQuoteString();
         if (tableName.startsWith(quote)) {
            if (!tableName.endsWith(quote)) {
               throw new IllegalStateException("Mismatched quote in table name: " + tableName);
            }
            int quoteLength = quote.length();
            tableName = tableName.substring(quoteLength, tableName.length() - quoteLength);
            if (dmd.storesLowerCaseQuotedIdentifiers()) {
               tableName = toLowerCase(tableName);
            } else if (dmd.storesUpperCaseQuotedIdentifiers()) {
               tableName = toUpperCase(tableName);
            }
         } else {
            if (dmd.storesLowerCaseIdentifiers()) {
               tableName = toLowerCase(tableName);
            } else if (dmd.storesUpperCaseIdentifiers()) {
               tableName = toUpperCase(tableName);
            }
         }

         int dotIndex;
         if ((dotIndex = tableName.indexOf('.')) != -1) {
            // Yank out schema name ...
            schema = tableName.substring(0, dotIndex);
            tableName = tableName.substring(dotIndex + 1);
         }

         rs = dmd.getTables(catalog, schema, tableName, null);
         return rs.next();
      }
      catch (SQLException e) {
         // This should not happen. A J2EE compatible JDBC driver is
         // required fully support meta data.
         throw new CacheLoaderException("Error while checking if table aleady exists " + tableName, e);
      }
      finally {
         JdbcUtil.safeClose(rs);
      }
   }

   public void createTable(Connection conn) throws CacheLoaderException {
      // removed CONSTRAINT clause as this causes problems with some databases, like Informix.
      assertMandatoryElemenetsPresent();
      String creatTableDdl = "CREATE TABLE " + getTableName() + "(" + idColumnName + " " + idColumnType
            + " NOT NULL, " + dataColumnName + " " + dataColumnType + ", "
            + timestampColumnName + " " + timestampColumnType +
            ", PRIMARY KEY (" + idColumnName + "))";
      if (log.isTraceEnabled())
         log.trace("Creating table with following DDL: '" + creatTableDdl + "'.");
      executeUpdateSql(conn, creatTableDdl);
   }

   private void assertMandatoryElemenetsPresent() throws CacheLoaderException {
      assrtNotNull(idColumnType, "idColumnType needed in order to create table");
      assrtNotNull(idColumnName, "idColumnName needed in order to create table");
      assrtNotNull(tableNamePrefix, "tableNamePrefix needed in order to create table");
      assrtNotNull(cacheName, "cacheName needed in order to create table");
      assrtNotNull(dataColumnName, "dataColumnName needed in order to create table");
      assrtNotNull(dataColumnType, "dataColumnType needed in order to create table");
      assrtNotNull(timestampColumnName, "timestampColumnName needed in order to create table");
      assrtNotNull(timestampColumnType, "timestampColumnType needed in order to create table");
   }

   private void assrtNotNull(String keyColumnType, String message) throws CacheLoaderException {
      if (keyColumnType == null || keyColumnType.trim().length() == 0) throw new CacheLoaderException(message);
   }

   private void executeUpdateSql(Connection conn, String sql) throws CacheLoaderException {
      Statement statement = null;
      try {
         statement = conn.createStatement();
         statement.executeUpdate(sql);
      } catch (SQLException e) {
         log.error("Error while creating table", e);
         throw new CacheLoaderException(e);
      } finally {
         JdbcUtil.safeClose(statement);
      }
   }

   public void dropTable(Connection conn) throws CacheLoaderException {
      String dropTableDdl = "DROP TABLE " + getTableName();
      String clearTable = "DELETE FROM " + getTableName();
      executeUpdateSql(conn, clearTable);
      if (log.isTraceEnabled())
         log.trace("Dropping table with following DDL '" + dropTableDdl + "\'");
      executeUpdateSql(conn, dropTableDdl);
   }

   private static String toLowerCase(String s) {
      return s.toLowerCase((Locale.ENGLISH));
   }

   private static String toUpperCase(String s) {
      return s.toUpperCase(Locale.ENGLISH);
   }

   public void setIdColumnName(String idColumnName) {
      this.idColumnName = idColumnName;
   }

   public void setIdColumnType(String idColumnType) {
      this.idColumnType = idColumnType;
   }

   public void setTableNamePrefix(String tableNamePrefix) {
      this.tableNamePrefix = tableNamePrefix;
   }

   public void setDataColumnName(String dataColumnName) {
      this.dataColumnName = dataColumnName;
   }

   public void setDataColumnType(String dataColumnType) {
      this.dataColumnType = dataColumnType;
   }

   public void setTimestampColumnName(String timestampColumnName) {
      this.timestampColumnName = timestampColumnName;
   }

   public void setTimestampColumnType(String timestampColumnType) {
      this.timestampColumnType = timestampColumnType;
   }

   public boolean isCreateTableOnStart() {
      return createTableOnStart;
   }

   public void setCreateTableOnStart(boolean createTableOnStart) {
      this.createTableOnStart = createTableOnStart;
   }

   public boolean isDropTableOnExit() {
      return dropTableOnExit;
   }

   public void setDropTableOnExit(boolean dropTableOnExit) {
      this.dropTableOnExit = dropTableOnExit;
   }

   public void start(ConnectionFactory connectionFactory) throws CacheLoaderException {
      this.connectionFactory = connectionFactory;
      if (isCreateTableOnStart()) {
         Connection conn = this.connectionFactory.getConnection();
         try {
            if (!tableExists(conn, getTableName())) {
               createTable(conn);
            }
         } finally {
            this.connectionFactory.releaseConnection(conn);
         }
      }
   }

   public void stop() throws CacheLoaderException {
      if (isDropTableOnExit()) {
         Connection conn = connectionFactory.getConnection();
         try {
            dropTable(conn);
         } finally {
            connectionFactory.releaseConnection(conn);
         }
      }
   }

   public String getInsertRowSql() {
      if (insertRowSql == null) {
         insertRowSql = "INSERT INTO " + getTableName() + " (" + dataColumnName + ", " + timestampColumnName + ", " + idColumnName + ") VALUES(?,?,?)";
      }
      return insertRowSql;
   }

   public String getUpdateRowSql() {
      if (updateRowSql == null) {
         updateRowSql = "UPDATE " + getTableName() + " SET " + dataColumnName + " = ? , " + timestampColumnName + "=? WHERE " + idColumnName + " = ?";
      }
      return updateRowSql;
   }

   public String getSelectRowSql() {
      if (selectRowSql == null) {
         selectRowSql = "SELECT " + idColumnName + ", " + dataColumnName + " FROM " + getTableName() + " WHERE " + idColumnName + " = ?";
      }
      return selectRowSql;
   }

   public String getDeleteRowSql() {
      if (deleteRowSql == null) {
         deleteRowSql = "DELETE FROM " + getTableName() + " WHERE " + idColumnName + " = ?";
      }
      return deleteRowSql;
   }

   public String getLoadAllRowsSql() {
      if (loadAllRowsSql == null) {
         loadAllRowsSql = "SELECT " + dataColumnName + "," + idColumnName + " FROM " + getTableName();
      }
      return loadAllRowsSql;
   }

   public String getDeleteAllRowsSql() {
      if (deleteAllRows == null) {
         deleteAllRows = "DELETE FROM " + getTableName();
      }
      return deleteAllRows;
   }

   public String getSelectExpiredRowsSql() {
      if (selectExpiredRowsSql == null) {
         selectExpiredRowsSql = getLoadAllRowsSql() + " WHERE " + timestampColumnName + "< ?";
      }
      return selectExpiredRowsSql;
   }

   public String getDeleteExpiredRowsSql() {
      if (deleteExpiredRowsSql == null) {
         deleteExpiredRowsSql = "DELETE FROM " + getTableName() + " WHERE " + timestampColumnName + "< ? AND " + timestampColumnName + "> 0";
      }
      return deleteExpiredRowsSql;
   }

   @Override
   public TableManipulation clone() {
      try {
         return (TableManipulation) super.clone();
      } catch (CloneNotSupportedException e) {
         throw new IllegalStateException(e);
      }
   }

   public String getTableName() {
      if (tableName == null) {
         if (tableNamePrefix == null || cacheName == null) {
            throw new IllegalStateException("Both tableNamePrefix and cacheName must be non null at this point!");
         }
         tableName = tableNamePrefix + "_" + cacheName;
      }
      return tableName;
   }

   public String getTableNamePrefix() {
      return tableNamePrefix;
   }

   public boolean tableExists(Connection connection) throws CacheLoaderException {
      return tableExists(connection, tableName);
   }

   public String getIdColumnName() {
      return idColumnName;
   }

   public String getIdColumnType() {
      return idColumnType;
   }

   public String getDataColumnName() {
      return dataColumnName;
   }

   public String getDataColumnType() {
      return dataColumnType;
   }

   public String getTimestampColumnName() {
      return timestampColumnName;
   }

   public String getTimestampColumnType() {
      return timestampColumnType;
   }

   /**
    * For DB queries (e.g. {@link org.infinispan.loaders.CacheStore#toStream(java.io.ObjectOutput)} ) the fetch size
    * will be set on {@link java.sql.ResultSet#setFetchSize(int)}. This is optional parameter, if not specified will be
    * defaulted to {@link #DEFAULT_FETCH_SIZE}.
    */
   public int getFetchSize() {
      return fetchSize;
   }

   /**
    * @see #getFetchSize()
    */
   public void setFetchSize(int fetchSize) {
      this.fetchSize = fetchSize;
   }

   /**
    * When doing repetitive DB inserts (e.g. on {@link org.infinispan.loaders.CacheStore#fromStream(java.io.ObjectInput)}
    * this will be batched according to this parameter. This is an optional parameter, and if it is not specified it
    * will be defaulted to {@link #DEFAULT_BATCH_SIZE}.
    */
   public int getBatchSize() {
      return batchSize;
   }

   /**
    * @see #getBatchSize()
    */
   public void setBatchSize(int batchSize) {
      this.batchSize = batchSize;
   }

   public void setCacheName(String cacheName) {
      this.cacheName = cacheName;
      this.tableName = null;
   }
}
