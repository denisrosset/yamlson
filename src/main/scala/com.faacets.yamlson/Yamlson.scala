package com.faacets.yamlson

import java.io.File
import java.io.{Reader, StringReader, InputStream, BufferedReader, InputStreamReader, FileInputStream}

import scala.collection.JavaConverters._
import scala.collection.mutable.Builder

import resource._

import play.api.libs.json._

import org.yaml.snakeyaml._
import resolver.Resolver
import reader.UnicodeReader
import constructor.SafeConstructor
import nodes.{Tag, NodeId, ScalarNode}
import events._

/**
 * Helper functions to parse YAML format as JsValues.
 */
object Yamlson {

  protected sealed trait Context {
    def parent: Context
  }

  protected case class RootContext(var documentsOption: Option[Seq[JsValue]] = None) extends Context {
    def parent = sys.error("Cannot get parent of root context")
    def setDocuments(documents: Seq[JsValue]): Unit = {
      require(documentsOption.isEmpty)
      documentsOption = Some(documents)
    }
  }

  protected case class StreamContext(parent: RootContext, documents: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends Context {
    def append(value: JsValue) = { documents += value }
    def result(): Seq[JsValue] = documents.result
  }

  protected sealed trait CollectionContext extends Context {
    def append(value: JsValue): Unit
    def result(): JsValue
  }

  protected case class DocumentContext(parent: StreamContext, elements: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends CollectionContext {
    def append(value: JsValue) = { elements += value }
    def result(): JsValue = {
      val seq = elements.result
      assert(seq.size == 1)
      seq.head
    }
  }

  protected case class SequenceContext(parent: CollectionContext, elements: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends CollectionContext {
    def append(value: JsValue) = { elements += value }
    def result(): JsValue = JsArray(elements.result)
  }

  protected case class MappingContext(parent: CollectionContext, elements: Builder[(String, JsValue), Seq[(String, JsValue)]] = Vector.newBuilder[(String, JsValue)], var keyRead: Option[String] = None) extends CollectionContext {
    def append(value: JsValue) = (value, keyRead) match {
      case (JsString(key), None) =>
        keyRead = Some(key)
      case (value, Some(key)) =>
        elements += (key -> value)
        keyRead = None
      case _ => throw new RuntimeException(s"Invalid key $value")
    }
    def result(): JsValue = JsObject(elements.result)
  }

  val resolver = new Resolver
  object Constructor extends SafeConstructor {
    def constructScalarNode(node: ScalarNode): AnyRef = {
      val constructor = getConstructor(node)
      constructor.construct(node)
    }
  }
  protected def processScalar(event: ScalarEvent): JsValue = {
    val tag = resolver.resolve(NodeId.scalar, event.getValue, true)
    val node = new ScalarNode(tag, true, event.getValue,
      event.getStartMark, event.getEndMark, event.getStyle)
    Constructor.constructScalarNode(node) match {
      case l: java.lang.Long => JsNumber(BigDecimal(l))
      case i: java.lang.Integer => JsNumber(BigDecimal(i))
      case bi: java.math.BigInteger => JsNumber(BigDecimal(bi))
      case other => JsString(event.getValue)
    }
  }
  protected def process(event: Event, context: Context): Context =
    (event, context) match {
      case (_: StreamStartEvent, root: RootContext) => StreamContext(root)
      case (_: StreamEndEvent, stream: StreamContext) =>
        stream.parent.setDocuments(stream.result)
        stream.parent
      case (_: DocumentStartEvent, stream: StreamContext) => DocumentContext(stream)
      case (_: DocumentEndEvent, document: DocumentContext) =>
        document.parent.append(document.result)
        document.parent
      case (_: MappingStartEvent, collection: CollectionContext) => MappingContext(collection)
      case (_: MappingEndEvent, mapping: MappingContext) =>
        mapping.parent.append(mapping.result)
        mapping.parent
      case (_: SequenceStartEvent, collection: CollectionContext) => SequenceContext(collection)
      case (_: SequenceEndEvent, sequence: SequenceContext) =>
        sequence.parent.append(sequence.result)
        sequence.parent
      case (scalar: ScalarEvent, collection: CollectionContext) =>
        collection.append(processScalar(scalar))
        collection
      case _ => sys.error(s"Wrong event $event in context $context")
    }

  def parseAll(reader: Reader): Seq[JsValue] = {
    val yaml = new Yaml
    val RootContext(Some(documents)) = ((RootContext(): Context) /: yaml.parse(reader).asScala) { case (context, event) => process(event, context) }
    documents
  }
  def parse(reader: Reader): JsValue = parseAll(reader).head

  def parseAll(input: String): Seq[JsValue] = parseAll(new StringReader(input))
  def parse(input: String): JsValue = parseAll(input).head

  def parseAll(input: InputStream): Seq[JsValue] = parseAll(new UnicodeReader(input))
  def parse(input: InputStream): JsValue = parseAll(input).head

  /**
   * Parse a byte array representing a json, and return it as a JsValue.
   *
   * The character encoding used will be automatically detected as UTF-8, UTF-16 or UTF-32, as per the heuristics in
   * RFC-4627.
   *
   * @param input a byte array to parse
   * @return the JsValue representing the byte array
   */
  def parseAll(input: Array[Byte]): Seq[JsValue] = parseAll(new java.io.ByteArrayInputStream(input))
  def parse(input: Array[Byte]): JsValue = parseAll(input).head

  def parseAll(file: File): Seq[JsValue] = {
    val result = managed(new FileInputStream(file)).map(new UnicodeReader(_)).map(new BufferedReader(_)).map(parseAll)
    result.opt.get
  }
  def parse(file: File): JsValue = parseAll(file).head


  protected def convertToPlainJavaTypes(value: JsValue): AnyRef = {
    value match {
      case JsNull => null
      case _: JsUndefined => throw new IllegalArgumentException("Undefined values are not supported.")
      case JsBoolean(b) => new java.lang.Boolean(b)
      case JsNumber(n) => n.toBigIntExact.getOrElse(throw new IllegalArgumentException("Floating point numbers are not supported")).bigInteger
      case JsString(s) => s
      case JsArray(seq) => seq.map(convertToPlainJavaTypes(_)).asJava
      case JsObject(fields) => collection.immutable.ListMap(fields.map { case (k, v) => (k, convertToPlainJavaTypes(v)) }: _*).asJava
    }
  }

  /**
   * Convert a JsValue to its YAML string representation.
   *
   *
   * @param json the JsValue to convert
   * @return a String with the YAML representation
   */
  def stringify(json: JsValue): String = {
    val options = new DumperOptions
    val yaml = new Yaml(options)
    yaml.dump(convertToPlainJavaTypes(json))
  }

  /**
   * Convert a sequence of JsValue to its YAML string representation as several documents.
   *
   *
   * @param json the JsValue to convert
   * @return a String with the YAML representation
   */
  def stringify(json: Seq[JsValue]): String = {
    val options = new DumperOptions
    val yaml = new Yaml(options)
    yaml.dumpAll(json.map(convertToPlainJavaTypes(_)).asJava.iterator)
  }

  //We use unicode \u005C for a backlash in comments, because Scala will replace unicode escapes during lexing
  //anywhere in the program.
  /**
   * Convert a JsValue to its YAML string representation, escaping all non-ascii characters using \u005CuXXXX syntax.
   *
   * @param json the JsValue to convert
   * @return a String with the YAML representation with all non-ascii characters escaped.
   */
  def asciiStringify(json: JsValue): String = {
    val options = new DumperOptions
    options.setAllowUnicode(false)
    val yaml = new Yaml(options)
    yaml.dump(convertToPlainJavaTypes(json))
  }

  def asciiStringify(json: Seq[JsValue]): String = {
    val options = new DumperOptions
    options.setAllowUnicode(false)
    val yaml = new Yaml(options)
    yaml.dump(json.map(convertToPlainJavaTypes(_)).asJava.iterator)
  }
}
