package jasition.matching.domain.trade

import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntryQuantity
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.client.Client

fun sameFirmButPossibleFirmAgainstClient(client: Client, other: Client): Boolean =
    client.firmId == other.firmId
            && (client.firmClientId == null || other.firmClientId == null)

fun sameFirmAndSameFirmClient(client: Client, other: Client): Boolean =
    client == other

fun priceHasCrossed(aggressor: BookEntry, passive: BookEntry): Boolean {
    val aggressorPrice = aggressor.key.price
    val passivePrice = passive.key.price

    return if (aggressorPrice != null && passivePrice != null)
        aggressor.side.comparatorMultipler() * aggressorPrice.compareTo(passivePrice) <= 0
    else findTradePrice(aggressorPrice, passivePrice) != null
}

fun findTradePrice(aggressor: Price?, passive: Price?): Price? = passive ?: aggressor

fun getTradeSize(aggressor: EntryQuantity, passive: EntryQuantity) : Int =
    Integer.min(aggressor.availableSize, passive.availableSize)

fun notAvailableForTrade(aggressor: EntryQuantity) : Boolean = aggressor.availableSize <= 0