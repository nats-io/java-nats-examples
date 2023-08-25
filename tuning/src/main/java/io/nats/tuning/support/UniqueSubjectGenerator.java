// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.support;

import io.nats.client.NUID;

public class UniqueSubjectGenerator implements SubjectGenerator {
    public final String subjectPrefix;
    public final String deliverPrefix;

    public UniqueSubjectGenerator() {
        this("sub-" + NUID.nextGlobalSequence() + ".", "del-");
    }

    public UniqueSubjectGenerator(String subjectPrefix, String deliverPrefix) {
        this.subjectPrefix = subjectPrefix;
        this.deliverPrefix = deliverPrefix;
    }

    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    public String getStreamSubject() {
        return subjectPrefix + ">";
    }

    public String getSubject(Object id) {
        return subjectPrefix + id;
    }

    @Override
    public String getNextDeliverSubject() {
        return deliverPrefix + NUID.nextGlobal();
    }
}
