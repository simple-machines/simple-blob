package au.com.simplemachines.blob.s3

private[s3] object FilenameUtils extends FilenameUtils

/**Lifted from commons FilenameUtils. */
private[s3] trait FilenameUtils {
  val ExtensionSeparator: Char = '.'
  val UnixSeparator = '/'
  val WindowsSeparator = '\\'


  def getName(filename: String): String = {
    if (filename == null) {
      return null
    }
    val index: Int = indexOfLastSeparator(filename)
    filename.substring(index + 1)
  }

  def getExtension(filename: String): String = {
    if (filename == null) return null

    val index = indexOfExtension(filename)
    if (index == -1) "" else filename.substring(index + 1)
  }

  def indexOfExtension(filename: String): Int = {
    if (filename == null) return -1

    val extensionPos = filename.lastIndexOf(ExtensionSeparator)
    val lastSeparator = indexOfLastSeparator(filename)
    if (lastSeparator > extensionPos) -1 else extensionPos
  }

  def indexOfLastSeparator(filename: String): Int = {
    if (filename == null) {
      return -1
    }
    val lastUnixPos: Int = filename.lastIndexOf(UnixSeparator)
    val lastWindowsPos: Int = filename.lastIndexOf(WindowsSeparator)
    Math.max(lastUnixPos, lastWindowsPos)
  }
}