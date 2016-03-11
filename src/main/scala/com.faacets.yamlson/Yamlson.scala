package com.faacets.yamlson

import java.io.File
import java.io.{Reader, StringReader, InputStream, BufferedReader, InputStreamReader, FileInputStream}

import scala.collection.JavaConverters._
import scala.collection.mutable.Builder

import resource._

import argonaut._, Argonaut._

import org.yaml.snakeyaml._
import reader.UnicodeReader

/**
 * Helper functions to parse YAML format as Jsons.
 */
object Yamlson {

  def parseAll(reader: Reader): Seq[Json] = {
    val yaml = new Yaml
    val machine = new StateMachine
    yaml.parse(reader).asScala.foreach( e => machine.event(e) )
    machine.result()
  }
  def parse(reader: Reader): Json = parseAll(reader).head

  def parseAll(input: String): Seq[Json] = parseAll(new StringReader(input))
  def parse(input: String): Json = parseAll(input).head

  def parseAll(input: InputStream): Seq[Json] = parseAll(new UnicodeReader(input))
  def parse(input: InputStream): Json = parseAll(input).head

  /**
   * Parse a byte array representing a json, and return it as a Json.
   *
   * The character encoding used will be automatically detected as UTF-8, UTF-16 or UTF-32, as per the heuristics in
   * RFC-4627.
   *
   * @param input a byte array to parse
   * @return the Json representing the byte array
   */
  def parseAll(input: Array[Byte]): Seq[Json] = parseAll(new java.io.ByteArrayInputStream(input))
  def parse(input: Array[Byte]): Json = parseAll(input).head

  def parseAll(file: File): Seq[Json] = {
    val result = managed(new FileInputStream(file)).map(new UnicodeReader(_)).map(new BufferedReader(_)).map(parseAll)
    result.opt.get
  }

  def parse(file: File): Json = parseAll(file).head

  import collection.immutable.ListMap
  def jsonObjectToListMap(jo: JsonObject): ListMap[String, Json] =
    ListMap(jo.fields.map( field => (field, jo(field).get) ): _*)


  protected def convertToPlainJavaTypes(value: Json): AnyRef =
    value.fold(
      jsonNull = null,
      jsonBool = new java.lang.Boolean(_),
      jsonNumber = _.toBigInt.getOrElse(throw new IllegalArgumentException("Floating point numbers are not supported")).bigInteger,
      jsonString = identity,
      jsonArray = _.map(convertToPlainJavaTypes).asJava,
      jsonObject = jsonObjectToListMap(_).mapValues(convertToPlainJavaTypes).asJava
    )

  /**
   * Convert a Json to its YAML string representation.
   *
   *
   * @param json the Json to convert
   * @return a String with the YAML representation
   */
  def stringify(json: Json): String = {
    val options = new DumperOptions
    val yaml = new Yaml(options)
    yaml.dump(convertToPlainJavaTypes(json))
  }

  /**
   * Convert a sequence of Json to its YAML string representation as several documents.
   *
   *
   * @param json the Json to convert
   * @return a String with the YAML representation
   */
  def stringify(json: Seq[Json]): String = {
    val options = new DumperOptions
    val yaml = new Yaml(options)
    yaml.dumpAll(json.map(convertToPlainJavaTypes(_)).asJava.iterator)
  }

  //We use unicode \u005C for a backlash in comments, because Scala will replace unicode escapes during lexing
  //anywhere in the program.
  /**
    * Convert a Json to its YAML string representation, escaping all non-ascii characters using \u005CuXXXX syntax.
    *
    * @param json the Json to convert
    * @return a String with the YAML representation with all non-ascii characters escaped.
    */
  def asciiStringify(json: Json): String = {
    val options = new DumperOptions
    options.setAllowUnicode(false)
    val yaml = new Yaml(options)
    yaml.dump(convertToPlainJavaTypes(json))
  }

  def asciiStringify(json: Seq[Json]): String = {
    val options = new DumperOptions
    options.setAllowUnicode(false)
    val yaml = new Yaml(options)
    yaml.dump(json.map(convertToPlainJavaTypes(_)).asJava.iterator)
  }

}
