package jasition.matching.domain.scenario.trading

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntriesRemovedFromBookEvent
import jasition.matching.domain.quote.command.QuoteModelType
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import java.time.Instant

internal class `Given the book has a Mass Quote of BUY 4 at 9 SELL 4 at 10 and BUY 5 at 8 SELL 5 at 11` :
    FeatureSpec({
        val quoteEntryModel = "[1 - Quote entry model] "
        val cannotMatchOtherMarketMaker = "[2 -  Cannot match other market maker] "
        val aggressorPartialFilledByQuotes = "[3 -  Aggressor order partial-filled by quotes] "
        val aggressorFilledByQuotes = "[4 -  Aggressor order filled by quotes] "

        val now = Instant.now()
        val bookId = aBookId()
        val originalMassQuotePlacedEvent = MassQuotePlacedEvent(
            bookId = bookId,
            eventId = EventId(1),
            whenHappened = now,
            quoteId = randomId(),
            whoRequested = aFirmWithoutClient(),
            quoteModelType = QuoteModelType.QUOTE_ENTRY,
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            entries = List.of(
                aQuoteEntryId(
                    bid = PriceWithSize(size = 4, price = Price(9)),
                    offer = PriceWithSize(size = 4, price = Price(10))
                ), aQuoteEntryId(
                    bid = PriceWithSize(size = 5, price = Price(8)),
                    offer = PriceWithSize(size = 5, price = Price(11))
                )
            )
        )
        val books = originalMassQuotePlacedEvent.play(aBooks(bookId)).aggregate


        feature(quoteEntryModel) {
            scenario(quoteEntryModel + "When a Mass Quote with Quote Entry mode of the same firm is placed, then all existing quotes of the same firm are cancelled and all new quote entires are added") {
                val massQuotePlacedEvent = MassQuotePlacedEvent(
                    bookId = bookId,
                    eventId = EventId(6),
                    whenHappened = now,
                    quoteId = randomId(),
                    whoRequested = aFirmWithoutClient(),
                    quoteModelType = QuoteModelType.QUOTE_ENTRY,
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    entries = List.of(
                        aQuoteEntryId(
                            bid = PriceWithSize(size = 5, price = Price(8)),
                            offer = PriceWithSize(size = 5, price = Price(9))
                        ), aQuoteEntryId(
                            bid = PriceWithSize(size = 6, price = Price(7)),
                            offer = PriceWithSize(size = 6, price = Price(10))
                        )
                    )
                )
                val result = massQuotePlacedEvent.play(books)

                val expectedBuyEntry1 = expectedBookEntry(
                    event = massQuotePlacedEvent,
                    eventId = EventId(8),
                    entry = massQuotePlacedEvent.entries.get(0),
                    side = Side.BUY,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
                val expectedSellEntry1 = expectedBookEntry(
                    event = massQuotePlacedEvent,
                    eventId = EventId(9),
                    entry = massQuotePlacedEvent.entries.get(0),
                    side = Side.SELL,
                    sizes = EntrySizes(5),
                    status = EntryStatus.NEW
                )
                val expectedBuyEntry2 = expectedBookEntry(
                    event = massQuotePlacedEvent,
                    eventId = EventId(10),
                    entry = massQuotePlacedEvent.entries.get(1),
                    side = Side.BUY,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                )
                val expectedSellEntry2 = expectedBookEntry(
                    event = massQuotePlacedEvent,
                    eventId = EventId(11),
                    entry = massQuotePlacedEvent.entries.get(1),
                    side = Side.SELL,
                    sizes = EntrySizes(6),
                    status = EntryStatus.NEW
                )

                result.events shouldBe List.of(
                    EntriesRemovedFromBookEvent(
                        eventId = EventId(7),
                        bookId = bookId,
                        whenHappened = now,
                        entries = List.of(
                            expectedBookEntry(event = originalMassQuotePlacedEvent,
                                eventId = EventId(2),
                                entry = originalMassQuotePlacedEvent.entries.get(0),
                                side = Side.BUY,
                                sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                                status = EntryStatus.CANCELLED)
                            ,
                            expectedBookEntry(event = originalMassQuotePlacedEvent,
                                eventId = EventId(4),
                                entry = originalMassQuotePlacedEvent.entries.get(1),
                                side = Side.BUY,
                                sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                                status = EntryStatus.CANCELLED)
                            ,
                            expectedBookEntry(event = originalMassQuotePlacedEvent,
                                eventId = EventId(3),
                                entry = originalMassQuotePlacedEvent.entries.get(0),
                                side = Side.SELL,
                                sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                                status = EntryStatus.CANCELLED)
                            ,
                            expectedBookEntry(event = originalMassQuotePlacedEvent,
                                eventId = EventId(5),
                                entry = originalMassQuotePlacedEvent.entries.get(1),
                                side = Side.SELL,
                                sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                                status = EntryStatus.CANCELLED)
                            )
                    ),
                    expectedBuyEntry1.toEntryAddedToBookEvent(bookId),
                    expectedSellEntry1.toEntryAddedToBookEvent(bookId),
                    expectedBuyEntry2.toEntryAddedToBookEvent(bookId),
                    expectedSellEntry2.toEntryAddedToBookEvent(bookId)
                )
                result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBuyEntry1, expectedBuyEntry2)
                result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedSellEntry1, expectedSellEntry2)
            }
        }

    })

