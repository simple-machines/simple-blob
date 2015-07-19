package au.com.simplemachines.blob

import java.time.Instant

/**
 * Metadata associated with a Blob.
 */
case class BInfo(key: BKey, mimeType: String, name: String, lastModified: Instant, size: Long)
