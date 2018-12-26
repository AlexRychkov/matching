package jasition.matching.domain

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row

internal class EventIdTest : StringSpec({
    "Cannot be negative"{
        shouldThrow<IllegalArgumentException> {
            EventId(-1)
        }
    }
    "Increments the value by one for the next Event ID"{
        EventId(1).next() shouldBe EventId(2)
    }
    "Rotates the next value to zero if the current Event ID is the maximum Long value"{
        EventId(Long.MAX_VALUE).next() shouldBe EventId(0)
    }
    "Recognises that Event ID (n + 1) is the next value of n"{
        forall(
            row(8L, 6L, false),
            row(8L, 7L, true),
            row(8L, 8L, false),
            row(8L, 9L, false),
            row(8L, 10L, false)
        ) { a, b, result ->
            EventId(a).isNextOf(EventId(b)) shouldBe result
        }
    }
    "Recognises that Event ID 0 is the next value of the maximum Long value"{
        EventId(0).isNextOf(EventId(Long.MAX_VALUE)) shouldBe true
    }
    "Evaluates that bigger Event ID is after smaller"{
        forall(
            row(8L, 6L, 1),
            row(8L, 7L, 1),
            row(8L, 8L, 0),
            row(8L, 9L, -1),
            row(8L, 10L, -1)
        ) { a, b, result ->
            EventId(a).compareTo(EventId(b)) shouldBe result
        }
        EventId(8).compareTo(EventId(6)) shouldBe 1
    }
    "Evaluates that Event ID Long.MAX_VALUE is before 0"{
        EventId(Long.MAX_VALUE).compareTo(EventId(0)) shouldBe -1
    }
    "Evaluates that Event ID 0 is after Long.MAX_VALUE"{
        EventId(0).compareTo(EventId(Long.MAX_VALUE)) shouldBe 1
    }
})