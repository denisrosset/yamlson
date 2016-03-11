package com.faacets.yamlson

import argonaut._, Argonaut._

import org.yaml.snakeyaml._
import events._

class StateMachine {

  import StateMachine._

  private[this] var context: Context = new RootContext

  def event(event: Event): Unit = {
    val newContext = (event, context) match {
      case (_: StreamStartEvent, root: RootContext) => new StreamContext(root)
      case (_: StreamEndEvent, stream: StreamContext) =>
        stream.parent.setDocuments(stream.result)
        stream.parent
      case (_: DocumentStartEvent, stream: StreamContext) => new DocumentContext(stream)
      case (_: DocumentEndEvent, document: DocumentContext) =>
        document.parent.append(document.result)
        document.parent
      case (_: MappingStartEvent, collection: CollectionContext) => new MappingContext(collection)
      case (_: MappingEndEvent, mapping: MappingContext) =>
        mapping.parent.append(mapping.result)
        mapping.parent
      case (_: SequenceStartEvent, collection: CollectionContext) => new SequenceContext(collection)
      case (_: SequenceEndEvent, sequence: SequenceContext) =>
        sequence.parent.append(sequence.result)
        sequence.parent
      case (scalar: ScalarEvent, collection: CollectionContext) =>
        collection.append(ScalarProcessor(scalar))
        collection
      case _ => sys.error(s"Wrong event $event in context $context")
    }
    context = newContext
  }

  def result(): Seq[Json] = context match {
    case (rc: RootContext) => rc.result()
    case _ => sys.error(s"Trying the end the parse in the wrong context $context")
  }

}

object StateMachine {

  /** Default builder type. */
  type ListBuilder[A] = scala.collection.mutable.Builder[A, List[A]]

  /** Default builder returing `Vector`. */
  object ListBuilder {

    def apply[A]: ListBuilder[A] = List.newBuilder[A]

  }

  /** Represents a context in the SnakeYAML parsing. */
  sealed trait Context

  /** Represents a context that is not a root node. */
  sealed trait ChildContext extends Context {

    def parent: Context

  }

  final class RootContext extends Context {

    private[this] var documentsOption: Option[Seq[Json]] = None

    def setDocuments(documents: Seq[Json]): Unit = {
      require(documentsOption.isEmpty)
      documentsOption = Some(documents)
    }

    def result(): Seq[Json] = documentsOption match {
      case Some(documents) => documents
      case None => throw new RuntimeException("Documents not set.")
    }

  }

  final class StreamContext(val parent: RootContext) extends ChildContext {

    val documents: ListBuilder[Json] = ListBuilder[Json]

    def append(value: Json) = { documents += value }

    def result(): Seq[Json] = documents.result()

  }

  sealed trait CollectionContext extends Context {

    type Element

    val elements: ListBuilder[Element] = ListBuilder[Element]

    def append(value: Json): Unit

    def result(): Json

  }

  final class DocumentContext(val parent: StreamContext) extends CollectionContext {

    type Element = Json

    def append(value: Json) = { elements += value }

    def result() = {
      val seq = elements.result()
      assert(seq.size == 1)
      seq.head
    }

  }

  final class SequenceContext(val parent: CollectionContext) extends CollectionContext {

    type Element = Json

    def append(value: Json) = { elements += value }

    def result() = jArray(elements.result())

  }

  final class MappingContext(val parent: CollectionContext) extends CollectionContext {

    type Element = (String, Json)
    private[this] var keyRead: Option[String] = None

    def append(scalar: Json) = (scalar.string, keyRead)  match {
      case (Some(newKey), None) => keyRead = Some(newKey)
      case (_, Some(key)) =>
        elements += (key -> scalar)
        keyRead = None
      case _ => throw new RuntimeException(s"Invalid key $scalar")
    }

    def result(): Json = {
      assert(keyRead.isEmpty)
      jObjectFields(elements.result(): _*)
    }

  }

}
