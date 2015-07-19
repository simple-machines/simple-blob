package au.com.simplemachines.blob.s3

/**
 * Summary information for an S3 object.
 *
 * @param bucketName The name of the Amazon S3 bucket in which this object is stored.
 * @param key The key under which this object is stored in Amazon S3.
 * @param size The size of this object in bytes.
 **/
case class S3ObjectSummary(bucketName: String, key: String, size: Int)
