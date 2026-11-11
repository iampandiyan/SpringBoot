package com.example.trade.kafka;

// Topic names in one place, so the producer, consumer and tests cannot disagree.
public final class KafkaTopics {

    public static final String TRADE_EVENTS = "trade-events";
    public static final String TRADE_EVENTS_DLT = "trade-events.DLT";     // Spring's default name: "<topic>.DLT"

    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_EVENT_TYPE = "eventType";
    public static final String HEADER_CREATED_AT = "createdAt";

    private KafkaTopics() {}
}
