package au.com.simplemachines.blob.s3

/**
 * Meta data information for an S3 object.
 *
 * @param bucketName The name of the Amazon S3 bucket in which this object is stored.
 * @param key The key under which this object is stored in Amazon S3.
 * @param name The name with which this object is stored in Amazon S3.
 * @param size The size of this object in bytes.
 * @param contentType The content-type of the object.
 * @param lastModified The last modified time of the object.
 **/
case class ObjectMetadata(bucketName: String, key: String, name: String, size: Int, contentType: String, lastModified: Long)
