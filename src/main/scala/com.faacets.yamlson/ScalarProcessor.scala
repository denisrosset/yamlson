package com.faacets.yamlson

import argonaut._, Argonaut._

import org.yaml.snakeyaml._
import events._

object ScalarProcessor {

  import constructor.SafeConstructor
  import nodes.{Tag, NodeId, ScalarNode}
  import resolver.Resolver

  private[this] val res = new Resolver

  private[this] object Constructor extends SafeConstructor {
    def constructScalarNode(node: ScalarNode): AnyRef = {
      val constructor = getConstructor(node)
      constructor.construct(node)
    }
  }

  def apply(event: ScalarEvent): Json = {
    val tag = res.resolve(NodeId.scalar, event.getValue, true)
    val node = new ScalarNode(tag, true, event.getValue,
      event.getStartMark, event.getEndMark, event.getStyle)
    Constructor.constructScalarNode(node) match {
      case b: java.lang.Boolean => jBool(b:Boolean)
      case l: java.lang.Long => jNumber(l:Long)
      case i: java.lang.Integer => jNumber(i:Int)
      case bi: java.math.BigInteger => jNumber(BigDecimal(bi))
      case other => jString(event.getValue)
        // TODO: support decimal/floating-point types
    }
  }

}
