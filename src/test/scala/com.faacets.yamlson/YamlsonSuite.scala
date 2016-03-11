package com.faacets.yamlson

import org.scalatest.{FunSuite, Matchers}

/**
  * An opinionated stack of traits to improve consistency and reduce boilerplate.
  * Thanks typelevel/cats for the idea.
  */
trait YamlsonSuite extends FunSuite with Matchers
