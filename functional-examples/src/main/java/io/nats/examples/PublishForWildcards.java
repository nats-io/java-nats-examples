package io.nats.examples;

import io.nats.client.Connection;
import io.nats.client.Nats;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PublishForWildcards {
    public static void main(String[] args) {

        try {
            // [begin wildcard_tester]
            Connection nc = Nats.connect("nats://demo.nats.io:4222");
            ZoneId zoneId = ZoneId.of("America/New_York");
            ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.now(), zoneId);
            String formatted = zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);

            nc.publish("time.us.east", formatted.getBytes(StandardCharsets.UTF_8));
            nc.publish("time.us.east.atlanta", formatted.getBytes(StandardCharsets.UTF_8));

            zoneId = ZoneId.of("Europe/Warsaw");
            zonedDateTime = ZonedDateTime.ofInstant(Instant.now(), zoneId);
            formatted = zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
            nc.publish("time.eu.east", formatted.getBytes(StandardCharsets.UTF_8));
            nc.publish("time.eu.east.warsaw", formatted.getBytes(StandardCharsets.UTF_8));

            nc.flush(Duration.ZERO);
            nc.close();
            // [end wildcard_tester]
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}