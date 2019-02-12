package jasition.matching.domain.book.entry

import jasition.cqrs.Transaction
import jasition.cqrs.playAndAppend
import jasition.cqrs.thenPlay_2_
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.MatchingResult

enum class TimeInForce(val code: String) {
    GOOD_TILL_CANCEL("GTC") {
        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                return if (canStayOnBook(aggressor.sizes))
                    transaction.copy(aggregate = transaction.aggregate.addBookEntry(aggressor))
                else
                    transaction
            }
        }

        override fun finalise_2_(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                with(transaction) {
                    return if (canStayOnBook(aggressor.sizes)) thenPlay_2_(
                        EntryAddedToBookEvent(
                            bookId = aggregate.bookId,
                            eventId = aggregate.lastEventId.next(),
                            entry = aggressor
                        )
                    ) else
                        this

                }
            }
        }

        override fun canStayOnBook(size: EntrySizes): Boolean = size.available > 0
    },

    IMMEDIATE_OR_CANCEL("IOC") {
        override fun finalise_2_(result: MatchingResult): Transaction<BookId, Books> = finalise(result)

        override fun finalise(result: MatchingResult): Transaction<BookId, Books> {
            with(result) {
                return if (aggressor.sizes.available > 0) {
                    with(transaction) {
                        return append(
                            aggressor.toOrderCancelledByExchangeEvent(
                                eventId = aggregate.lastEventId.next(),
                                bookId = aggregate.bookId
                            ) playAndAppend aggregate
                        )
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
    @Deprecated("Old CQRS semantics")
    abstract fun finalise(result: MatchingResult): Transaction<BookId, Books>

    /**
     * Finalises a [MatchingResult] and turns it into a [Transaction]. This is the final acceptance check
     * and post-processing of the [MatchingResult]. Depends on the [TimeInForce], the [MatchingResult]
     * may be accepted or rejected. Also post-processing may include adding the entry of remaining size to
     * the book, cancelling the remaining size of the aggressor, or cancelling the whole aggressor or
     * reverting the whole [MatchingResult].
     */
    abstract fun finalise_2_(result: MatchingResult): Transaction<BookId, Books>
}