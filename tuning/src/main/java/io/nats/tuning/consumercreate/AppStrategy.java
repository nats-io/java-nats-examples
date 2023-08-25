// Copyright (C) 2023 Synadia Communications, Inc.
// This file is part of Synadia Communications, Inc.'s
// private Java-Nats tooling. The "tuning" project can not be
// copied and/or distributed without the express permission
// of Synadia Communications, Inc.

package io.nats.tuning.consumercreate;

public enum AppStrategy {
    Individual_Immediately,
    Individual_After_Creates,
    Client_Api_Subscribe,
    Do_Not_Sub
}
