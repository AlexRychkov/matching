package jasition.matching.domain.book

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Aggregate
import jasition.cqrs.EventId
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.BookEntryKey
import jasition.matching.domain.book.entry.Side
import java.time.LocalDate
import java.util.function.Function
import java.util.function.Predicate

data class BookId(val bookId: String)

data class Books(
    val bookId: BookId,
    val buyLimitBook: LimitBook = LimitBook(Side.BUY),
    val sellLimitBook: LimitBook = LimitBook(Side.SELL),
    val businessDate: LocalDate = LocalDate.now(),
    val tradingStatuses: TradingStatuses = TradingStatuses(TradingStatus.OPEN_FOR_TRADING),
    val lastEventId: EventId = EventId(0)
) : Aggregate<BookId> {
    override fun aggregateId(): BookId = bookId

    fun addBookEntry(entry: BookEntry): Books = copy(
        buyLimitBook = if (Side.BUY == entry.side) buyLimitBook.add(entry) else buyLimitBook,
        sellLimitBook = if (Side.SELL == entry.side) sellLimitBook.add(entry) else sellLimitBook,
        lastEventId = entry.key.eventId
    )

    fun removeBookEntries(eventId: EventId, entries: Seq<BookEntry>): Books =
        copy(
            buyLimitBook = buyLimitBook.removeAll(entries),
            sellLimitBook = sellLimitBook.removeAll(entries),
            lastEventId = eventId
        )

    fun updateBookEntry(
        eventId: EventId,
        side: Side,
        bookEntryKey: BookEntryKey,
        updater: Function<BookEntry, BookEntry>
    ): Books =
        copy(
            buyLimitBook = if (Side.BUY == side) buyLimitBook.update(bookEntryKey, updater) else buyLimitBook,
            sellLimitBook = if (Side.SELL == side) sellLimitBook.update(bookEntryKey, updater) else sellLimitBook,
            lastEventId = eventId
        )

    fun removeBookEntry(
        eventId: EventId,
        side: Side,
        bookEntryKey: BookEntryKey
    ): Books =
        copy(
            buyLimitBook = if (Side.BUY == side) buyLimitBook.remove(bookEntryKey) else buyLimitBook,
            sellLimitBook = if (Side.SELL == side) sellLimitBook.remove(bookEntryKey) else sellLimitBook,
            lastEventId = eventId
        )

    fun findBookEntries(predicate: Predicate<BookEntry>): List<BookEntry> =
        List.ofAll(
            buyLimitBook.entries.filterValues(predicate).values()
                .appendAll(
                    sellLimitBook.entries.filterValues(predicate).values()
                )
        )

    fun verifyEventId(eventId: EventId): EventId =
        if (eventId.isNextOf(lastEventId)) eventId
        else throw IllegalArgumentException("Incoming Entry is not the next expected event ID. lastEventId=$lastEventId, incomingEventId=$eventId")
}
