package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import kotlin.math.absoluteValue


internal class `Earlier entries have priority over later` : StringSpec({
    val bookId = aBookId()

    forall(
        row(BUY, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), -1L, true),
        row(BUY, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), 0L, false),
        row(BUY, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), 1L, false),
        row(SELL, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), -1L, true),
        row(SELL, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), 0L, false),
        row(SELL, Tuple2(LIMIT, GOOD_TILL_CANCEL), Tuple2(LIMIT, GOOD_TILL_CANCEL), 1L, false)
    ) { side, old, new, newRequestTimeOffsetMillis, expectNewOverOld ->
        "Given the book has a $side ${old.a} ${old.b.code} order, when a $side ${new.a} ${new.b.code} order requested ${newRequestTimeOffsetMillis.absoluteValue} millis ${if (newRequestTimeOffsetMillis < 0) "earlier" else "later"} is placed, then the new entry is added ${if (expectNewOverOld) "above" else "below"} the old"{
            val oldCommand = randomPlaceOrderCommand(
                bookId = bookId,
                side = side,
                entryType = old.a,
                timeInForce = old.b
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = list(oldCommand))
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = side,
                entryType = new.a,
                timeInForce = new.b,
                price = oldCommand.price,
                whenRequested = oldCommand.whenRequested.plusMillis(newRequestTimeOffsetMillis)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntry = expectedBookEntry(oldCommand, EventId(1))
            val newBookEntry = expectedBookEntry(command, EventId(3))

            with(result) {
                events shouldBe list(
                    expectedOrderPlacedEvent(command, EventId(3)),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(4), entry = newBookEntry)
                )
            }
            repo.read(bookId).let {
                with(command.side) {
                    sameSideBook(it).entries.values() shouldBe
                            if (expectNewOverOld)
                                list(newBookEntry, oldBookEntry)
                            else
                                list(oldBookEntry, newBookEntry)
                    oppositeSideBook(it).entries.size() shouldBe 0
                }
            }
        }
    }
})

