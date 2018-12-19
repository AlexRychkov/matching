package jasition.matching.domain.order.event

import arrow.core.Tuple2
import jasition.matching.domain.Event
import jasition.matching.domain.book.BookEntry
import jasition.matching.domain.book.BookEntryKey
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.addBookEntry
import jasition.matching.domain.order.Client
import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
import java.time.Instant

data class OrderPlacedEvent(
    val id: Long,
    val requestId: String,
    val whoRequested: Client,
    val bookId: String,
    val orderType: OrderType,
    val side: Side,
    val availableSize: Int,
    val tradedSize: Int,
    val cancelledSize: Int,
    val price: Long?,
    val timeInForce: TimeInForce,
    val whenHappened: Instant
) : Event

fun convertOrderPlacedEventToBookEntry(event: OrderPlacedEvent): BookEntry {
    return BookEntry(
        key = BookEntryKey(
            price = event.price,
            whenSubmitted = event.whenHappened,
            entryEventId = event.id
        ),
        clientEntryId = event.requestId,
        client = event.whoRequested,
        orderType = event.orderType,
        side = event.side,
        availableSize = event.availableSize,
        tradedSize = event.tradedSize,
        cancelledSize = event.cancelledSize,
        timeInForce = event.timeInForce
    )
}

fun playOrderPlacedEvent(event: OrderPlacedEvent, books: Books): Tuple2<List<Event>, Books> {
    return Tuple2(emptyList(), addBookEntry(books, convertOrderPlacedEventToBookEntry(event)))
}