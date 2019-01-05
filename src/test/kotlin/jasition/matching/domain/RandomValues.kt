package jasition.matching.domain

import io.vavr.collection.Array
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.command.PlaceMassQuoteCommand
import java.time.Instant
import kotlin.random.Random


fun randomId(prefix: String = "req", from: Int = 1, to: Int = 1000): String = prefix + Random.nextInt(from, to)

fun randomSize(from: Int = 10, until: Int = 30): Int = Random.nextInt(from, until)

fun randomPrice(from: Long = 20, until: Long = 30): Price = Price(Random.nextLong(from = from, until = until))

fun randomFirmWithClient(): Client = Client(
    firmId = randomId(prefix = "firm", from = 1, to = 5),
    firmClientId = randomId(prefix = "client", from = 1, to = 5)
)

fun randomPlaceOrderCommand(
    requestId: ClientRequestId = aClientRequestId(current = randomId()),
    bookId: BookId = aBookId(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = if (Random.nextBoolean()) Side.BUY else Side.SELL,
    price: Price? = randomPrice(),
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    whenRequested: Instant = Instant.now(),
    whoRequested: Client = randomFirmWithClient(),
    size: Int = randomSize()
) = PlaceOrderCommand(
    requestId = requestId,
    bookId = bookId,
    entryType = entryType,
    side = side,
    price = price,
    timeInForce = timeInForce,
    whenRequested = whenRequested,
    whoRequested = whoRequested,
    size = size
)

fun randomPlaceMassQuoteCommand(
    quoteId: String = randomId(),
    bookId: BookId = aBookId(),
    depth: Int = 5,
    minBuy: Price = Price(15),
    maxBuy: Price = Price(27),
    minSell: Price = Price(26),
    maxSell: Price = Price(35),
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    whenRequested: Instant = Instant.now(),
    whoRequested: Client = randomFirmWithClient()
): PlaceMassQuoteCommand {

    val entryList = Array.range(0, depth)
        .map {
            aQuoteEntry(
                bid = PriceWithSize(randomPrice(from = minBuy.value, until = maxBuy.value), randomSize()),
                offer = PriceWithSize(randomPrice(from = minSell.value, until = maxSell.value), randomSize())
            )
        }
    return PlaceMassQuoteCommand(
        quoteId = quoteId,
        bookId = bookId,
        timeInForce = timeInForce,
        whenRequested = whenRequested,
        whoRequested = whoRequested,
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        entries = entryList
    )


}