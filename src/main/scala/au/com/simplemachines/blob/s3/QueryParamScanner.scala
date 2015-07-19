package au.com.simplemachines.blob.s3

import java.util.Scanner
import collection.JavaConversions._

private[s3] object QueryParamScanner {
  val ParamSep = "&"
  val KvSep = "="

  def parse(uri: String): List[(String, String)] = {
    parse(new Scanner(uri))
  }

  private def parse(scanner: Scanner) = {
    scanner.useDelimiter(ParamSep)
    scanner.map {_.split(KvSep)}
      .filterNot {_.length != 2}
      .map {kv => (kv(0), kv(1))}
      .toList
  }

}