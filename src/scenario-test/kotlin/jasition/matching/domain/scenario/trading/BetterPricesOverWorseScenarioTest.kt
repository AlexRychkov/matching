package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.event.EntryAddedToBookEvent


internal class `Better prices over worse` : FeatureSpec({
    val bookId = aBookId()

    feature("Better prices over worse") {
        forall(
            row(BUY, Tuple3(LIMIT, GOOD_TILL_CANCEL , 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL, 11L), true),
            row(BUY, Tuple3(LIMIT, GOOD_TILL_CANCEL, 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL, 9L), false),
            row(SELL, Tuple3(LIMIT, GOOD_TILL_CANCEL , 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL,9L), true),
            row(SELL, Tuple3(LIMIT, GOOD_TILL_CANCEL, 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL, 11L), false)
        ) { side, old, new, expectNewOverOld ->
            scenario(
                "Given the book has a $side ${old.a} ${old.b.code} order at ${old.c}, " +
                        "when a $side ${new.a} ${new.b.code} order at ${new.c} is placed, " +
                        "then the new entry is added ${if (expectNewOverOld) "above" else "below"} the old"
            ) {
                val oldCommand = randomPlaceOrderCommand(
                    bookId = bookId,
                    side = side,
                    entryType = old.a,
                    timeInForce = old.b,
                    price = Price(old.c)
                )
                val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
                val command = randomPlaceOrderCommand(
                    bookId = bookId,
                    side = side,
                    entryType = new.a,
                    timeInForce = new.b,
                    price = Price(new.c)
                )

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                val oldBookEntry = expectedBookEntry(oldCommand, EventId(2))
                val newBookEntry = expectedBookEntry(command, EventId(4))

                with(result) {
                    events shouldBe List.of(
                        expectedOrderPlacedEvent(command, EventId(3)),
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(4), entry = newBookEntry)
                    )
                }
                repo.read(bookId).let {
                    with(command.side) {
                        sameSideBook(it).entries.values() shouldBe
                                if (expectNewOverOld)
                                    List.of(newBookEntry, oldBookEntry)
                                else
                                    List.of(oldBookEntry, newBookEntry)
                        oppositeSideBook(it).entries.size() shouldBe 0
                    }
                }
            }
        }
    }
})

