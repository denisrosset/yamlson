package com.faacets.yamlson

import org.yaml.snakeyaml._
import events._
import java.io.File
import java.io.{Reader, StringReader, InputStream, BufferedReader, InputStreamReader, FileInputStream}
import scala.collection.JavaConversions._
import scala.collection.mutable.Builder
import play.api.libs.json._
import resource._

sealed trait Context {
  def parent: Context
}

case class RootContext(var documentsOption: Option[Seq[JsValue]] = None) extends Context {
  def parent = sys.error("Cannot get parent of root context")
  def setDocuments(documents: Seq[JsValue]): Unit = {
    require(documentsOption.isEmpty)
    documentsOption = Some(documents)
  }
}

case class StreamContext(parent: RootContext, documents: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends Context {
  def append(value: JsValue) = { documents += value }
  def result(): Seq[JsValue] = documents.result
}

sealed trait CollectionContext extends Context {
  def append(value: JsValue): Unit
  def result(): JsValue
}

case class DocumentContext(parent: StreamContext, elements: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends CollectionContext {
  def append(value: JsValue) = { elements += value }
  def result(): JsValue = {
    val seq = elements.result
    assert(seq.size == 1)
    seq.head
  }
}

case class SequenceContext(parent: CollectionContext, elements: Builder[JsValue, Seq[JsValue]] = Vector.newBuilder[JsValue]) extends CollectionContext {
  def append(value: JsValue) = { elements += value }
  def result(): JsValue = JsArray(elements.result)
}

case class MappingContext(parent: CollectionContext, elements: Builder[(String, JsValue), Seq[(String, JsValue)]] = Vector.newBuilder[(String, JsValue)], var keyRead: Option[String] = None) extends CollectionContext {
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

/**
 * Helper functions to parse YAML format as JsValues.
 */
object Yamlson {
  def process(event: Event, context: Context): Context =
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
        collection.append(JsString(scalar.getValue))
        collection
      case _ => sys.error(s"Wrong event $event in context $context")
    }

  def parseAll(reader: Reader): Seq[JsValue] = {
    val yaml = new Yaml
    val RootContext(Some(documents)) = ((RootContext(): Context) /: yaml.parse(reader)) { case (context, event) => process(event, context) }
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
}
