// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.consumercreate;

public enum SubStrategy {
    Push_Without_Stream(false),
    Push_Provide_Stream(false),
    Push_Bind(false),
    Pull_Without_Stream(true),
    Pull_Provide_Stream(true),
    Pull_Bind(true);

    public final boolean pull;

    SubStrategy(boolean pull) {
        this.pull = pull;
    }
}
