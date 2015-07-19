package au.com.simplemachines.blob

import java.net.URLConnection

class UrlMimeTypeLookup extends MimeTypeLookup {
  def resolveMimeTypeForExtension(ext: String, defaultType: String): String = {
    // FileNameMap is cached out of the URLConnection.getFileNameMap call.
    val fileNameMap = URLConnection.getFileNameMap

    // Hack a pretend filename back onto the file.
    val contentType = fileNameMap.getContentTypeFor("foo." + ext)

    Option(contentType).getOrElse(defaultType)
  }
}
