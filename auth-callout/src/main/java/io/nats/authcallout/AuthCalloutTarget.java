// Copyright 2024 The NATS Authors
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

package io.nats.authcallout;

import io.nats.jwt.Permission;
import io.nats.jwt.ResponsePermission;

public class AuthCalloutTarget {
    public String user;
    public String pass;
    public String authToken;
    public String account;
    public Permission pub;
    public Permission sub;
    public ResponsePermission resp;

    public AuthCalloutTarget userPass(String userPass) {
        this.user = userPass;
        this.pass = userPass;
        return this;
    }

    public AuthCalloutTarget authToken(String authToken) {
        this.authToken = authToken;
        return this;
    }

    public AuthCalloutTarget user(String user) {
        this.user = user;
        return this;
    }

    public AuthCalloutTarget pass(String pass) {
        this.pass = pass;
        return this;
    }

    public AuthCalloutTarget account(String account) {
        this.account = account;
        return this;
    }

    public AuthCalloutTarget pub(Permission pub) {
        this.pub = pub;
        return this;
    }

    public AuthCalloutTarget sub(Permission sub) {
        this.sub = sub;
        return this;
    }

    public AuthCalloutTarget resp(ResponsePermission resp) {
        this.resp = resp;
        return this;
    }
}
