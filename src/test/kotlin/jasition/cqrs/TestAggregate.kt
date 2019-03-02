package jasition.cqrs


internal class TestAggregate(
    val aggregateId: Int = 1,
    val value: String = "test"
) : Aggregate<Int> {
    override fun aggregateId(): Int = aggregateId
}

internal data class TestEvent(
    val aggregateId: Int = 1,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: TestAggregate): TestAggregate = aggregate
}

internal data class TestPrimaryEvent(
    val aggregateId: Int = 1,
    val eventId: EventId
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: TestAggregate): TestAggregate = aggregate
}

internal data class TestPrimaryEvent2(
    val aggregateId: Int = 1,
    val eventId: EventId,
    val updatedAggregate: TestAggregate,
    val sideEffectEvent: TestEvent
) : Event<Int, TestAggregate> {
    override fun aggregateId(): Int = aggregateId
    override fun eventId(): EventId = eventId
    override fun play(aggregate: TestAggregate): TestAggregate = aggregate
}