package jasition.matching.domain.book.entry

import io.vavr.collection.List
import jasition.cqrs.Transaction
import jasition.cqrs.append
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.MatchingResult

enum class TimeInForce(val code: String) {
    GOOD_TILL_CANCEL("GTC") {
        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                with(transaction) {
                    return if (canStayOnBook(aggressor.sizes)) {
                        val eventId = aggregate.lastEventId.inc()
                        val addedEvent = EntryAddedToBookEvent(
                            bookId = aggregate.bookId,
                            eventId = eventId,
                            entry = aggressor
                        )

                        val addedBooks = addedEvent.play(aggregate)
                        append(Transaction<BookId, Books>(aggregate = addedBooks, events = List.of(addedEvent)))
                    } else
                        this

                }
            }
        }

        override fun canStayOnBook(size: EntrySizes): Boolean = size.available > 0
    },

    IMMEDIATE_OR_CANCEL("IOC") {
        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                return if (aggressor.sizes.available > 0) {
                    with(transaction) {
                        val cancelledEvent = aggressor.toOrderCancelledByExchangeEvent(
                                eventId = aggregate.lastEventId.inc(),
                                bookId = aggregate.bookId

                        )

                        val cancelledBooks = cancelledEvent.play(aggregate)

                        append(Transaction<BookId, Books>(aggregate = cancelledBooks, events = List.of(cancelledEvent)))
                    }
                } else transaction
            }
        }

        override fun canStayOnBook(size: EntrySizes): Boolean = false
    };

    abstract fun canStayOnBook(size: EntrySizes): Boolean

    /**
     * Finalises a [MatchingResult] and turns it into a [Transaction]. This is the final acceptance check
     * and post-processing of the [MatchingResult]. Depends on the [TimeInForce], the [MatchingResult]
     * may be accepted or rejected. Also post-processing may include adding the entry of remaining size to
     * the book, cancelling the remaining size of the aggressor, or cancelling the whole aggressor or
     * reverting the whole [MatchingResult].
     */
    abstract fun finalise(result: MatchingResult): Transaction<BookId, Books>
}