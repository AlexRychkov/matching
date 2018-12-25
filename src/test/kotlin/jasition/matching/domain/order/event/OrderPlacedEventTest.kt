package jasition.matching.domain.order.event

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.expectedBookEntry
import jasition.matching.domain.expectedEntryAddedToBookEvent
import java.time.Instant

internal class OrderPlacedEventPropertyTest : DescribeSpec() {
    init {
        val eventId = EventId(1)
        val bookId = BookId("book")
        val event = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(15),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now(),
            eventId = eventId,
            size = EntryQuantity(10)
        )
        describe("OrderPlacedEvent") {
            it("has Book ID as Aggregate ID") {
                event.aggregateId() shouldBe bookId
            }
            it("has Event ID as Event ID") {
                event.eventId() shouldBe eventId
            }
            it("is a Primary event") {
                event.eventType() shouldBe EventType.PRIMARY
            }
        }
    }
}

internal class OrderPlacedEventBehaviourTest : BehaviorSpec() {
    init {
        this.
        given("the book is empty") {
            val books = Books(BookId("book"))
            `when`("an order placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = books.bookId,
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(15),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(1),
                    size = EntryQuantity(10)
                )

                val result = orderPlacedEvent.play(books)

                then("has no effect on the opposite side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                then("adds the order to the book") {
                    val expectedBookEntry = expectedBookEntry(orderPlacedEvent)

                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
                    if (entryAddedToBookEvent is EntryAddedToBookEvent) {
                        entryAddedToBookEvent shouldBe expectedEntryAddedToBookEvent(
                            orderPlacedEvent, books, expectedBookEntry
                        )
                    }

                    result.aggregate.buyLimitBook.entries.size() shouldBe 1
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe expectedBookEntry
                }
            }
        }   
    }
}

