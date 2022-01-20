// Copyright 2021 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.example;

import io.nats.client.Message;
import io.nats.client.support.DateTimeUtils;

import java.time.ZonedDateTime;

public class Result {

    public static final String HDR_START_TIME_NANOS = "stnano";
    public static final String HDR_START_TIME_ZDT = "stzdt";
    public static final String HDR_MSG_ID = "msgid";

    final long publishTimeNanos;
    final ZonedDateTime publishDateTime;
    final ZonedDateTime messageTime;
    final long receivedTimeNanos;
    final ZonedDateTime receivedDateTime;
    final long afterAckTimeNanos;
    final ZonedDateTime afterAckDateTime;

    public Result(Message msg,
                  long receivedTimeNanos, ZonedDateTime receivedDateTime,
                  long afterAckTimeNanos, ZonedDateTime afterAckDateTime)
    {
        this.publishTimeNanos = Long.parseLong(msg.getHeaders().getFirst(HDR_START_TIME_NANOS));
        this.publishDateTime = DateTimeUtils.parseDateTime(msg.getHeaders().getFirst(HDR_START_TIME_ZDT));
        this.messageTime = msg.metaData().timestamp();
        this.receivedTimeNanos = receivedTimeNanos;
        this.receivedDateTime = receivedDateTime;
        this.afterAckTimeNanos = afterAckTimeNanos;
        this.afterAckDateTime = afterAckDateTime;
    }

    @Override
    public String toString() {
        return "pub:" + DateTimeUtils.toRfc3339(publishDateTime) +
            " | msg: " + DateTimeUtils.toRfc3339(messageTime) +
            " | rcvd: " + DateTimeUtils.toRfc3339(receivedDateTime) +
            " | ack:" + DateTimeUtils.toRfc3339(afterAckDateTime)
            ;
    }

    public long elapsedBetweenPublishAndServer() {
        return toMillis(messageTime) - toMillis(publishDateTime);
    }

    public long elapsedBetweenServerAndReceived() {
        return toMillis(receivedDateTime) - toMillis(messageTime);
    }

    public long elapsedBetweenReceivedAndAfterAck() {
        return toMillis(afterAckDateTime) - toMillis(receivedDateTime);
    }

    public long elapsedBetweenPublishAndReceived() {
        return toMillis(receivedDateTime) - toMillis(publishDateTime);
    }

    public long elapsedBetweenPublishAndAfterAck() {
        return toMillis(afterAckDateTime) - toMillis(publishDateTime);
    }

    public long nanosBetweenPublishAndReceived() {
        return receivedTimeNanos - publishTimeNanos;
    }

    public long nanosBetweenPublishAndAfterAck() {
        return afterAckTimeNanos - publishTimeNanos;
    }

    public long nanosBetweenReceivedAndAfterAck() {
        return afterAckTimeNanos - receivedTimeNanos;
    }

    private static long toMillis(ZonedDateTime zdt) {
        return zdt.toInstant().toEpochMilli();
    }
}
