package jasition.matching.domain.quote.event

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.cqrs.playAndAppend
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.cancelExistingQuotes
import jasition.matching.domain.trade.matchAndPlaceEntry
import java.time.Instant

data class MassQuotePlacedEvent(
    val eventId: EventId,
    val quoteId: String,
    val whoRequested: Client,
    val bookId: BookId,
    val quoteModelType: QuoteModelType,
    val timeInForce: TimeInForce,
    val entries: Seq<QuoteEntry>,
    val whenHappened: Instant
) : Event<BookId, Books> {
    override fun aggregateId(): BookId = bookId
    override fun eventId(): EventId = eventId
    override fun isPrimary(): Boolean = true

    override fun play(aggregate: Books): Transaction<BookId, Books> {
        val books = aggregate.copy(lastEventId = aggregate.verifyEventId(eventId))
        val event = cancelExistingQuotes(
            books = books,
            eventId = eventId,
            whoRequested = whoRequested,
            whenHappened = whenHappened,
            primary = false
        )

        val initial = if (event != null) event playAndAppend books else Transaction(books)

// TODO: revise for newer Jacoco version - Below is equivalence to above but Jacoco cannot reach 100% coverage with the let function
//        val initial = event?.playAndAppend(books) ?: Transaction(books)

        return toBookEntries().fold(initial) { txn, entry ->
            txn.append(matchAndPlaceEntry(entry, txn.aggregate))
        }
    }

    fun toBookEntries(
        offset: Int = 0,
        bookEntries: Seq<BookEntry> = List.empty()
    ): Seq<BookEntry> =
        if (offset >= entries.size())
            bookEntries
        else toBookEntries(
            offset = offset + 1,
            bookEntries = bookEntries.appendAll(
                entries.get(offset).toBookEntries(
                    whenHappened = whenHappened,
                    eventId = eventId,
                    whoRequested = whoRequested,
                    timeInForce = timeInForce,
                    quoteId = quoteId
                )
            )
        )
}