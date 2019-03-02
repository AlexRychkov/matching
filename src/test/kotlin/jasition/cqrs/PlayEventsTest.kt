package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.spyk
import io.vavr.collection.List

internal class PlayEventsTest : StringSpec({
    "Events are re-played incrementally and sequentially" {
        val aggregate1 = TestAggregate()
        val aggregate2 = TestAggregate()
        val aggregate3 = TestAggregate()
        val finalAggregate = TestAggregate()
        val event1 = spyk(TestPrimaryEvent(eventId = EventId(1)))
        val event2 = spyk(TestPrimaryEvent(eventId = EventId(2)))
        val event3 = spyk(TestPrimaryEvent(eventId = EventId(3)))

        every { event1.play(aggregate1) } returns aggregate2
        every { event2.play(aggregate2) } returns aggregate3
        every { event3.play(aggregate3) } returns finalAggregate

        val events: List<Event<Int, TestAggregate>> = List.of(event1, event2, event3)

        events play aggregate1 shouldBe finalAggregate
    }
})