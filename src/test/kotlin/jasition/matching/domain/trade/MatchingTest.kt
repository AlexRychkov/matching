package jasition.matching.domain.trade

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.mockk
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.trade.event.TradeEvent

internal class MatchingTest : StringSpec({
    val bookId = aBookId()
    val client = aFirmWithClient()
    val otherClient = anotherFirmWithClient()
    val existingEvents = List.of(mockk<OrderPlacedEvent>(), mockk<TradeEvent>())

    "Stop matching when there is no available sizes in the aggressor" {
        val books = aBooks(bookId, List.of(aBookEntry(side = Side.SELL, whoRequested = client)))
        val aggressor = aBookEntry(side = Side.BUY, whoRequested = otherClient, sizes = EntrySizes(0))
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "Stop matching when there is no more entries in the opposite-side book" {
        val books = aBooks(bookId, List.of(aBookEntry(side = Side.BUY, whoRequested = client)))
        val aggressor = aBookEntry(side = Side.BUY, whoRequested = otherClient)
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "Stop matching when there is no more next match in the opposite-side book" {
        val books = aBooks(bookId, List.of(aBookEntry(side = Side.SELL, whoRequested = client, price = Price(35))))
        val aggressor = aBookEntry(side = Side.BUY, whoRequested = otherClient, price = Price(30))
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = aggressor,
            transaction = Transaction(aggregate = books, events = existingEvents)
        )
    }
    "First match filled the aggressor and the passive entry in the opposite-side book" {
        val tradeSize = 15
        val tradePrice = Price(35)
        val passive = aBookEntry(
            side = Side.SELL,
            whoRequested = client,
            sizes = EntrySizes(tradeSize),
            price = tradePrice
        )
        val books = aBooks(bookId, List.of(passive))
        val aggressor = aBookEntry(
            side = Side.BUY,
            whoRequested = otherClient,
            sizes = EntrySizes(tradeSize),
            price = tradePrice
        )
        val tradedAggressor = aggressor.traded(tradeSize)
        val tradedPassive = passive.traded(tradeSize)
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = tradedAggressor,
            transaction = Transaction(
                aggregate = aBooks(bookId).copy(lastEventId = EventId(2)),
                events = existingEvents.append(
                    TradeEvent(
                        eventId = EventId(2),
                        bookId = bookId,
                        size = tradeSize,
                        price = tradePrice,
                        whenHappened = aggressor.key.whenSubmitted,
                        aggressor = tradedAggressor.toTradeSideEntry(),
                        passive = tradedPassive.toTradeSideEntry()
                    )
                )
            )
        )
    }
    "First match filled the aggressor and partially-filled the passive entry in the opposite-side book" {
        val tradeSize = 15
        val tradePrice = Price(35)
        val passive = aBookEntry(
            side = Side.SELL,
            whoRequested = client,
            sizes = EntrySizes(tradeSize + 5),
            price = tradePrice
        )
        val books = aBooks(bookId, List.of(passive))
        val aggressor = aBookEntry(
            side = Side.BUY,
            whoRequested = otherClient,
            sizes = EntrySizes(tradeSize),
            price = tradePrice
        )
        val tradedPassive = passive.traded(tradeSize)
        val tradedAggressor = aggressor.traded(tradeSize)
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = tradedAggressor,
            transaction = Transaction(
                aggregate = aBooks(bookId)
                    .addBookEntry(tradedPassive)
                    .copy(lastEventId = EventId(2)),
                events = existingEvents.append(
                    TradeEvent(
                        eventId = EventId(2),
                        bookId = bookId,
                        size = tradeSize,
                        price = Price(35),
                        whenHappened = aggressor.key.whenSubmitted,
                        aggressor = tradedAggressor.toTradeSideEntry(),
                        passive = tradedPassive.toTradeSideEntry()
                    )
                )
            )
        )
    }
    "First match partially-filled the aggressor and filled the first passive entry in the opposite-side book; Second match filled the aggressor and partially-filled the second passive entry" {
        val tradeSize = 15
        val tradeSize2 = 5
        val tradePrice = Price(35)
        val passive = aBookEntry(
            side = Side.SELL,
            whoRequested = client,
            sizes = EntrySizes(tradeSize),
            price = tradePrice
        )
        val passive2 = passive.copy(
            sizes = EntrySizes(tradeSize2 + 10),
            key = passive.key.copy(eventId = passive.key.eventId.next())
        )
        val books = aBooks(bookId, List.of(passive, passive2))
        val aggressor = aBookEntry(
            side = Side.BUY,
            whoRequested = otherClient,
            sizes = EntrySizes(tradeSize + tradeSize2),
            price = tradePrice
        )
        val tradedPassive = passive.traded(tradeSize)
        val tradedPassive2 = passive2.traded(tradeSize2)
        val tradedAggressor = aggressor.traded(tradeSize)
        val tradedAggressor2 = tradedAggressor.traded(tradeSize2)
        match(
            aggressor = aggressor,
            books = books,
            events = existingEvents
        ) shouldBe MatchResult(
            aggressor = tradedAggressor2,
            transaction = Transaction(
                aggregate = aBooks(bookId)
                    .addBookEntry(tradedPassive2)
                    .copy(lastEventId = EventId(4)),
                events = existingEvents.appendAll(
                    List.of(
                        TradeEvent(
                            eventId = EventId(3),
                            bookId = bookId,
                            size = tradeSize,
                            price = Price(35),
                            whenHappened = aggressor.key.whenSubmitted,
                            aggressor = tradedAggressor.toTradeSideEntry(),
                            passive = tradedPassive.toTradeSideEntry()
                        ),
                        TradeEvent(
                            eventId = EventId(4),
                            bookId = bookId,
                            size = tradeSize2,
                            price = Price(35),
                            whenHappened = aggressor.key.whenSubmitted,
                            aggressor = tradedAggressor2.toTradeSideEntry(),
                            passive = tradedPassive2.toTradeSideEntry()
                        )
                    )
                )
            )
        )
    }
})