package jasition.matching.domain.behaviour

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.expectedBookEntry
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Sell aggressor order trades with buy passive order if sell price is higher than or equal to sell price` :
    BehaviorSpec() {
    init {
        given("the book has a BUY Limit GTC Order 4@10") {
            val existingEntry = BookEntry(
                key = BookEntryKey(
                    price = Price(10),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),
                clientRequestId = ClientRequestId("oldReq1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(4),
                status = EntryStatus.NEW
            )
            val bookId = BookId("book")
            val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate
            `when`("a SELL Limit GTC Order 5@11 placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client2"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("has the existing entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 1
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                }
                then("adds the entry on the SELL side with expected order data") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 1
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
                        orderPlacedEvent
                    )
                }
            }
            `when`("a SELL Limit GTC Order 5@10 placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client2"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("generates a Trade 4@10 and no more side-effect event") {
                    result.events.size() shouldBe 2

                    val sideEffectEvent = result.events.get(0)
                    sideEffectEvent should beOfType<TradeEvent>()

                    if (sideEffectEvent is TradeEvent) {
                        sideEffectEvent.size shouldBe 4
                        sideEffectEvent.price shouldBe Price(10)
                    }
                }
                then("remove the existing entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                then("adds a Sell 1@10 on the SELL side") {
                    result.events.size() shouldBe 2
                    result.events.get(1) should beOfType<EntryAddedToBookEvent>()

                    val entryAddedToBookEvent = result.events.get(1)
                    if (entryAddedToBookEvent is EntryAddedToBookEvent) {
                        result.aggregate.sellLimitBook.entries.size() shouldBe 1
                        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
                            orderPlacedEvent
                        ).copy(
                            key = BookEntryKey(
                                price = orderPlacedEvent.price,
                                whenSubmitted = orderPlacedEvent.whenHappened,
                                eventId = entryAddedToBookEvent.eventId
                            ),
                            status = EntryStatus.PARTIAL_FILL,
                            size = EntryQuantity(
                                availableSize = 1,
                                tradedSize = 4,
                                cancelledSize = 0
                            )
                        )
                    }
                }
            }
        }
    }
}
