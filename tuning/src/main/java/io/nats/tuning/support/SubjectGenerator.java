// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.support;

public interface SubjectGenerator {
    String getSubjectPrefix();
    String getStreamSubject();
    String getSubject(Object id);
    String getNextDeliverSubject();
}
