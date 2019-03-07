package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.event.EntryAddedToBookEvent

internal class `Mass quote placed on and added to empty book` : StringSpec({
    val bookId = aBookId()

    forall(
        row(list(Tuple4(4, 10L, 4, 11L), Tuple4(5, 9L, 5, 12L)))
    ) { entries ->
        "Given an empty book, when a mass quote of (${quoteEntriesAsString(
            entries
        )}) is placed, then all quote entries are added" {
            val repo = aRepoWithABooks(bookId = bookId)
            val command = randomPlaceMassQuoteCommand(bookId = bookId, entries = entries)

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val expectedBookEntries = list(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = EventId(1), side = it.b) }

            with(result) {
                events shouldBe list(
                    expectedMassQuotePlacedEvent(command, EventId(1)),
                    EntryAddedToBookEvent(bookId, EventId(2), expectedBookEntries[0]),
                    EntryAddedToBookEvent(bookId, EventId(3), expectedBookEntries[1]),
                    EntryAddedToBookEvent(bookId, EventId(4), expectedBookEntries[2]),
                    EntryAddedToBookEvent(bookId, EventId(5), expectedBookEntries[3])
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.values() shouldBe list(expectedBookEntries[0], expectedBookEntries[2])
                it.sellLimitBook.entries.values() shouldBe list(expectedBookEntries[1], expectedBookEntries[3])
            }
        }
    }
})

