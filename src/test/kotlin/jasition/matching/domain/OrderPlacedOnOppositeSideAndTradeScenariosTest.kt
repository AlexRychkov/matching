package jasition.matching.domain

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.play
import jasition.matching.domain.trade.event.TradeEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object OrderPlacedOnOppositeSideAndTradeScenariosTest : Spek({
    context(": Matching Rule : Match if aggressor price is same or better than passive") {
        given("The book has a Buy Limit GTC Order 4@10") {
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
            val books = Books(BookId("book")).addBookEntry(existingEntry).aggregate
            on("a SELL Limit GTC Order 5@11 is placed") {
                val event = OrderPlacedEvent(
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
                val result = play(event, books)
                it("places the new order in the opposite book") {

                    result.aggregate.buyLimitBook.entries.size() shouldBe 1
                    result.aggregate.sellLimitBook.entries.size() shouldBe 1
                    result.events.size() shouldBe 0

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            on("a SELL Limit GTC Order 5@10 is placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = play(event, books)
                it("generates a Trade 4@10") {

                    result.events.size() shouldBe 1

                    val sideEffectEvent = result.events.get(0)

                    sideEffectEvent should beOfType<TradeEvent>()

                    if (sideEffectEvent is TradeEvent) {
                        sideEffectEvent.size shouldBe 4
                        sideEffectEvent.price shouldBe Price(10)
                    }
                }
                it("places a Sell 1@10 on the book") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                    result.aggregate.sellLimitBook.entries.size() shouldBe 1

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            /*
            on("a BUY Limit GTC Order 5@9 is placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                it("places the new order below the existing") {
                    val results = play(event, books)

                    results.aggregate.buyLimitBook.entries.size() shouldBe 2
                    results.aggregate.sellLimitBook.entries.size() shouldBe 0

                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
        }
    }


    context(": Able to maintain the priority of the Sell Limit book (Price ascending then time descending)") {
        given("The book has a Sell Limit GTC Order 4@10") {
            val existingEntry = BookEntry(
                key = BookEntryKey(
                    price = Price(10),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),
                clientRequestId = ClientRequestId("oldReq1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(4),
                status = EntryStatus.NEW
            )
            val books = Books(BookId("book")).addBookEntry(existingEntry).aggregate
            on("a SELL Limit GTC Order 5@11 is placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                it("places the new order below the existing") {
                    val results = play(event, books)

                    results.aggregate.buyLimitBook.entries.size() shouldBe 0
                    results.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            on("a SELL Limit GTC Order 5@10 is placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                it("places the new order below the existing") {
                    val results = play(event, books)

                    results.aggregate.buyLimitBook.entries.size() shouldBe 0
                    results.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            on("a SELL Limit GTC Order 5@9 is placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                it("places the new order above the existing") {
                    val results = play(event, books)

                    results.aggregate.buyLimitBook.entries.size() shouldBe 0
                    results.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                    assertEntry(
                        entry = results.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                }
            }*/
        }
    }
})
