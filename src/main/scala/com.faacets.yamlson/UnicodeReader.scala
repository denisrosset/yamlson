package com.faacets.yamlson

/* Taken from http://www.areaofthoughts.com/2011/08/java-utf-8-and-optional-byte-order-mark.html
 * version: 1.1 / 2007-01-25
 * - changed BOM recognition ordering (longer boms first)
 * 
 * Original pseudocode   : Thomas Weidenfeller
 * Implementation tweaked: Aki Nieminen
 * Conversion to Scala   : Denis Rosset
 * 
 * http://www.unicode.org/unicode/faq/utf_bom.html
 * BOMs:
 * 00 00 FE FF    = UTF-32, big-endian
 * FF FE 00 00    = UTF-32, little-endian
 * EF BB BF       = UTF-8,
 * FE FF          = UTF-16, big-endian
 * FF FE          = UTF-16, little-endian
 * 
 *  Win2k Notepad:
 *  Unicode format = UTF-16LE
 */

import java.io._

object UnicodeReader {
  private val BOM_SIZE: Int = 4
}

/**
  * Generic unicode textreader, which will use BOM mark
  * to identify the encoding to be used. If BOM is not found,
  * then use UTF-8.
  * 
  * @param in         inputstream to be read
  * @param defaultEnc default encoding if stream does not have
  *                   BOM marker. Give NULL to use system-level default.
  */
class UnicodeReader(in: InputStream, defaultEnc: String = "UTF-8") extends Reader {
  import UnicodeReader.BOM_SIZE
  val internalIn = new PushbackInputStream(in, BOM_SIZE)
  var internalIn2: Option[InputStreamReader] = None

 /**
   * Get stream encoding or None if stream is uninitialized.
   * Call init() or read() method to initialize it.
   */
  def encoding: Option[String] = internalIn2.map(_.getEncoding)

  /**
   * Read-ahead four bytes and check for BOM marks. Extra bytes are
   * unread back to the stream, only BOM bytes are skipped.
   */
  @throws(classOf[IOException])
  def init: Unit = internalIn2 match {
    case None =>
      val bom = new Array[Byte](BOM_SIZE)
      var n = 0
      n = internalIn.read(bom, 0, bom.length)
      val FE = 0xFE.toByte
      val EF = 0xEF.toByte
      val FF = 0xFF.toByte
      val BB = 0xBB.toByte
      val BF = 0xBF.toByte
      val (encoding, unread) = bom match {
        case Array(0, 0, FE, FF, _*) => ("UTF-32BE", n - 4)
        case Array(FF, FE, 0, 0, _*) => ("UTF-32LE", n - 4)
        case Array(EF, BB, BF, _*) => ("UTF-8", n - 3)
        case Array(FE, FF, _*) => ("UTF-16BE", n - 2)
        case Array(FF, FE, _*) => ("UTF-16LE", n - 2)
        case _ => (defaultEnc, n)
      }
      if (unread > 0) internalIn.unread(bom, (n - unread), unread)
      internalIn2 = Some(new InputStreamReader(internalIn, encoding))
    case _ =>
  }

  @throws(classOf[IOException])
  def close: Unit = internalIn2.map(_.close)

  @throws(classOf[IOException])
  def read(cbuf: Array[Char], off: Int, len: Int): Int = {
    init
    internalIn2.get.read(cbuf, off, len)
  }
}
