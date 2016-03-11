package com.faacets.yamlson

import play.api.libs.json._

class ParseTests extends YamlsonSuite {

  test("Positive max-int") {
    val yaml = "num: 2147483647"
    Yamlson.parse(yaml) shouldBe JsObject(Seq("num" -> JsNumber(BigDecimal("2147483647"))))
  }

  test("Object with numbers") {
    val yaml = """---
content:
  uri: "http://javaone.com/keynote.mpg"
  title: "Javaone Keynote"
  width: 640
  height: 480
  persons:
  - "Foo Bar"
  - "Max Power"
"""
    Yamlson.parse(yaml) shouldBe JsObject(Seq(
      "content" -> JsObject(Seq(
        "uri" -> JsString("http://javaone.com/keynote.mpg"),
        "title" -> JsString("Javaone Keynote"),
        "width" -> JsNumber(640),
        "height" -> JsNumber(480),
        "persons" -> JsArray(Seq(JsString("Foo Bar"), JsString("Max Power")))
      ))
    ))
  }

}

/*        String yaml = "nulls: [!!null \"null\" ]";
        String yaml = "nulls: [~ ]";*/

  /*
        // Test negative max-int
        yaml = "num: -2147483648";
        // Test positive max-int + 1
        yaml = "num: 2147483648";
        // Test negative max-int - 1
        yaml = "num: -2147483649";
        // Test positive max-long
        yaml = "num: 9223372036854775807";
        // Test negative max-long
        yaml = "num: -9223372036854775808";
        // Test positive max-long + 1
        yaml = "num: 9223372036854775808";
        // Test negative max-long - 1
        yaml = "num: -9223372036854775809";
    // [Issue-4]: accidental recognition as double, with multiple dots
        // First, test out valid use case.
        yaml = "num: +1_000.25"; // note underscores; legal in yaml apparently
        final String IP = "10.12.45.127";
        yaml = "ip: "+IP+"\n";
        // should be considered a String...
    // [Issue#10]
    // Scalars should not be parsed when not in the plain flow style.
        String yaml = "strings: [\"true\", 'false']";
    // Scalars should be parsed when in the plain flow style.
        String yaml = "booleans: [true, false]";
*/
