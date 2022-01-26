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

package io.nats.jstiming;

import io.nats.client.Message;
import io.nats.client.support.DateTimeUtils;

import java.time.ZonedDateTime;

import static io.nats.client.impl.JsTimingMessage.HDR_START_TIME_ZDT;

public class Result {

    final ZonedDateTime publishDateTime;
    final ZonedDateTime serverTime;
    final ZonedDateTime receivedDateTime;
    final ZonedDateTime afterAckDateTime;

    public Result(Message msg, ZonedDateTime receivedDateTime, ZonedDateTime afterAckDateTime)
    {
        this.publishDateTime = DateTimeUtils.parseDateTime(msg.getHeaders().getFirst(HDR_START_TIME_ZDT));
        this.serverTime = msg.metaData().timestamp();
        this.receivedDateTime = receivedDateTime;
        this.afterAckDateTime = afterAckDateTime;
    }

    @Override
    public String toString() {
        return "pub:" + DateTimeUtils.toRfc3339(publishDateTime) +
            " | srvr: " + DateTimeUtils.toRfc3339(serverTime) +
            " | rcvd: " + DateTimeUtils.toRfc3339(receivedDateTime) +
            " | ack:" + DateTimeUtils.toRfc3339(afterAckDateTime)
            ;
    }

    public static boolean DEBUG = false;

    private void debug(String label, ZonedDateTime zdt1, ZonedDateTime zdt2) {
        System.out.println(label + toMillis(zdt1) +
            " - " + toMillis(zdt2) +
            " = " + (toMillis(zdt1) - toMillis(zdt2)));
    }

    public long elapsedMillisBetweenPublishAndServer() {
//        if (DEBUG) { debug("  pub srvr: ", serverTime, publishDateTime); }
        return toMillis(serverTime) - toMillis(publishDateTime);
    }

    public long elapsedMillisBetweenServerAndReceived() {
//        if (DEBUG) { debug("  srvr rcvd: ", receivedDateTime, serverTime); }
        return toMillis(receivedDateTime) - toMillis(serverTime);
    }

    public long elapsedMillisBetweenReceivedAndAfterAck() {
//        if (DEBUG) { debug("  rcvd ack: ", afterAckDateTime, receivedDateTime); }
        return toMillis(afterAckDateTime) - toMillis(receivedDateTime);
    }

    public long elapsedMillisBetweenPublishAndReceived() {
//        if (DEBUG) { debug("  pub rcvd: ", receivedDateTime, publishDateTime); }
        return toMillis(receivedDateTime) - toMillis(publishDateTime);
    }

    public long elapsedMillisBetweenPublishAndAfterAck() {
//        if (DEBUG) { debug("  pub ack: ", afterAckDateTime, publishDateTime); }
        return toMillis(afterAckDateTime) - toMillis(publishDateTime);
    }

    private static long toMillis(ZonedDateTime zdt) {
        return zdt.toInstant().toEpochMilli();
    }
}
