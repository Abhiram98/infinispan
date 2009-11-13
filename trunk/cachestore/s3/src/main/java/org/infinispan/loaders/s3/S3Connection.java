package org.infinispan.loaders.s3;

import org.infinispan.marshall.Marshaller;

/**
 * Represents a connection to Amazon S3.
 *
 * @author Adrian Cole
 * @since 4.0
 */
public interface S3Connection<C, B> {

   void connect(S3CacheStoreConfig config, Marshaller m) throws S3ConnectionException;

   C getConnection() throws S3ConnectionException;

   B verifyOrCreateBucket(String bucketName) throws S3ConnectionException;

   void destroyBucket(String name) throws S3ConnectionException;

   void copyBucket(String sourceBucket, String destinationBucket) throws S3ConnectionException;

   void disconnect();
}