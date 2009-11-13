package org.infinispan.loaders.jdbc;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.jdbc.connectionfactory.ConnectionFactory;
import org.infinispan.marshall.Marshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * The purpose of this class is to factorize the repeating code between {@link org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStore}
 * and {@link org.infinispan.loaders.jdbc.binary.JdbcBinaryCacheStore}. This class implements GOF's template method pattern.
 *
 * @author Mircea.Markus@jboss.com
 */
public abstract class DataManiulationHelper {

   private static Log log = LogFactory.getLog(DataManiulationHelper.class);

   private ConnectionFactory connectionFactory;
   private TableManipulation tableManipulation;
   protected Marshaller marshaller;


   public DataManiulationHelper(ConnectionFactory connectionFactory, TableManipulation tableManipulation, Marshaller marshaller) {
      this.connectionFactory = connectionFactory;
      this.tableManipulation = tableManipulation;
      this.marshaller = marshaller;
   }

   public void clear() throws CacheLoaderException {
      Connection conn = null;
      PreparedStatement ps = null;
      try {
         String sql = tableManipulation.getDeleteAllRowsSql();
         conn = connectionFactory.getConnection();
         ps = conn.prepareStatement(sql);
         int result = ps.executeUpdate();
         if (log.isTraceEnabled())
            log.trace("Successfully removed " + result + " rows.");
      } catch (SQLException ex) {
         logAndThrow(ex, "Failed clearing JdbcBinaryCacheStore");
      } finally {
         JdbcUtil.safeClose(ps);
         connectionFactory.releaseConnection(conn);
      }
   }


   public final void fromStreamSupport(ObjectInput objectInput) throws CacheLoaderException {
      Connection conn = null;
      PreparedStatement ps = null;
      try {
         conn = connectionFactory.getConnection();
         String sql = tableManipulation.getInsertRowSql();
         ps = conn.prepareStatement(sql);

         int readCount = 0;
         int batchSize = tableManipulation.getBatchSize();

         Object objFromStream = marshaller.objectFromObjectStream(objectInput);
         while (fromStreamProcess(objFromStream, ps, objectInput)) {
            ps.addBatch();
            readCount++;
            if (readCount % batchSize == 0) {
               ps.executeBatch();
               if (log.isTraceEnabled())
                  log.trace("Executing batch " + (readCount / batchSize) + ", batch size is " + batchSize);
            }
            objFromStream = marshaller.objectFromObjectStream(objectInput);
         }
         if (readCount % batchSize != 0)
            ps.executeBatch();//flush the batch
         if (log.isTraceEnabled())
            log.trace("Successfully inserted " + readCount + " buckets into the database, batch size is " + batchSize);
      } catch (IOException ex) {
         logAndThrow(ex, "I/O failure while integrating state into store");
      } catch (SQLException e) {
         logAndThrow(e, "SQL failure while integrating state into store");
      } catch (ClassNotFoundException e) {
         logAndThrow(e, "Unexpected failure while integrating state into store");
      } finally {
         JdbcUtil.safeClose(ps);
         connectionFactory.releaseConnection(conn);
      }
   }


   public final void toStreamSupport(ObjectOutput objectOutput, byte streamDelimiter) throws CacheLoaderException {
      //now write our data
      Connection connection = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
         String sql = tableManipulation.getLoadAllRowsSql();
         if (log.isTraceEnabled()) log.trace("Running sql '" + sql);
         connection = connectionFactory.getConnection();
         ps = connection.prepareStatement(sql);
         rs = ps.executeQuery();
         rs.setFetchSize(tableManipulation.getFetchSize());
         while (rs.next()) {
            InputStream is = rs.getBinaryStream(1);
            toStreamProcess(rs, is, objectOutput);
         }
         marshaller.objectToObjectStream(streamDelimiter, objectOutput);
      } catch (SQLException e) {
         logAndThrow(e, "SQL Error while storing string keys to database");
      } catch (IOException e) {
         logAndThrow(e, "I/O Error while storing string keys to database");
      }
      finally {
         JdbcUtil.safeClose(rs);
         JdbcUtil.safeClose(ps);
         connectionFactory.releaseConnection(connection);
      }

   }


   public final Set<InternalCacheEntry> loadAllSupport() throws CacheLoaderException {
      Connection conn = null;
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
         String sql = tableManipulation.getLoadAllRowsSql();
         conn = connectionFactory.getConnection();
         ps = conn.prepareStatement(sql);
         rs = ps.executeQuery();
         rs.setFetchSize(tableManipulation.getFetchSize());
         Set<InternalCacheEntry> result = new HashSet<InternalCacheEntry>();
         while (rs.next()) {
            loadAllProcess(rs, result);
         }
         return result;
      } catch (SQLException e) {
         String message = "SQL error while fetching all StoredEntries";
         log.error(message, e);
         throw new CacheLoaderException(message, e);
      } finally {
         JdbcUtil.safeClose(rs);
         JdbcUtil.safeClose(ps);
         connectionFactory.releaseConnection(conn);
      }
   }

   public abstract void loadAllProcess(ResultSet rs, Set<InternalCacheEntry> result) throws SQLException, CacheLoaderException;

   public abstract void toStreamProcess(ResultSet rs, InputStream is, ObjectOutput objectOutput) throws CacheLoaderException, SQLException, IOException;

   public abstract boolean fromStreamProcess(Object objFromStream, PreparedStatement ps, ObjectInput objectInput) throws SQLException, CacheLoaderException, IOException, ClassNotFoundException;

   public static void logAndThrow(Exception e, String message) throws CacheLoaderException {
      log.error(message, e);
      throw new CacheLoaderException(message, e);
   }
}
