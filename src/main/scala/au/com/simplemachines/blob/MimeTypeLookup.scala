package au.com.simplemachines.blob

trait MimeTypeLookup {
  /** Lookup the mime-type for provided extension. Retrusn defaultType if no stream can be found. */
  def resolveMimeTypeForExtension(ext: String, defaultType: String): String
}
