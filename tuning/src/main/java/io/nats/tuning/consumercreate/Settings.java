// Copyright 2023 The NATS Authors
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

package io.nats.tuning.consumercreate;

import io.nats.client.Options;
import io.nats.client.api.StorageType;
import io.nats.tuning.support.OptionsBuilderFactory;
import io.nats.tuning.support.SubjectGenerator;
import io.nats.tuning.support.UniqueSubjectGenerator;

public class Settings {

    public OptionsBuilderFactory optionsBuilder = () ->
        Options.builder().server("localhost:4222,localhost:5222,localhost:6222");

    public long verifyConnectMs = 10000; // < 0 means do not verify

    public String streamName = "stream";
    public StorageType storageType = StorageType.File;
    public int replicas = 3;

    public SubjectGenerator subjectGenerator = new UniqueSubjectGenerator();

    public long timeoutMs = 120_000; // 2 minutes

    public int appInstances = 10;
    public int threadsPerApp = 1;
    public int consumersPerApp = 100;
    public long beforeCreateDelayMs = 20;
    public long beforeSubDelayMs = 20;

    public long inactiveThresholdMs = timeoutMs * 2;
    public SubBehavior subBehavior = SubBehavior.After_Creates;

    public int publishInstances = 10;
    public int payloadSize = 100;
    public int pauseAfterStartPublishingMs = 2000;

    public int reportFrequency = Math.max(1, (int) (consumersPerApp / threadsPerApp * 0.1));
}
