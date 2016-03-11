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

  def parseAll(reader: Reader): Seq[JsValue] = {
    val yaml = new Yaml
    val machine = new StateMachine
    yaml.parse(reader).asScala.foreach( e => machine.event(e) )
    machine.result()
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
