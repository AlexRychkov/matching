package jasition.matching.domain.trade.event

import jasition.matching.domain.Event
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

data class TradeEvent(
    val eventId: EventId,
    val bookId: BookId,
    val size: Int,
    val price: Price,
    val whenHappened: Instant,
    val aggressor: TradeSideEntry,
    val passive: TradeSideEntry
) : Event {
    override fun eventId(): EventId = eventId
    override fun type(): EventType = EventType.SIDE_EFFECT
}

data class TradeSideEntry(
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val entryType: EntryType,
    val side: Side,
    val size: EntryQuantity,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenSubmitted: Instant,
    val entryEventId: EventId,
    val entryStatus: EntryStatus
) {
    fun toBookEntryKey(): BookEntryKey =
        BookEntryKey(price = price, whenSubmitted = whenSubmitted, eventId = entryEventId)

}

fun trade(event: TradeEvent, books: Books): Transaction<Books> = Transaction(
    books.withEventId(books.verifyEventId(event.eventId))
        .traded(event.aggressor)
        .traded(event.passive)

)

