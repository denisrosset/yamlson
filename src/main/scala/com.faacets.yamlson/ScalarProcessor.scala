package com.faacets.yamlson

import play.api.libs.json._

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

  def apply(event: ScalarEvent): JsValue = {
    val tag = res.resolve(NodeId.scalar, event.getValue, true)
    val node = new ScalarNode(tag, true, event.getValue,
      event.getStartMark, event.getEndMark, event.getStyle)
    Constructor.constructScalarNode(node) match {
      case l: java.lang.Long => JsNumber(BigDecimal(l))
      case i: java.lang.Integer => JsNumber(BigDecimal(i))
      case bi: java.math.BigInteger => JsNumber(BigDecimal(bi))
      case other => JsString(event.getValue)
    }
  }

}
