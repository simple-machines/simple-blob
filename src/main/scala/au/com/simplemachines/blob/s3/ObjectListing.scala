package au.com.simplemachines.blob.s3

/**
 * Result of listing an S3 bucket.
 *
 * @param bucketName The name of the Amazon S3 bucket containing the objects listed in this ObjectListing.
 * @param objectSummaries The list of object summaries describing the objects stored in the S3 bucket.
 **/
case class ObjectListing(bucketName: String, objectSummaries: Iterable[S3ObjectSummary])
