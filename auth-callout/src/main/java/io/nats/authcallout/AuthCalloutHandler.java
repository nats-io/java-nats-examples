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

import io.nats.client.Connection;
import io.nats.client.impl.Headers;
import io.nats.jwt.*;
import io.nats.nkey.NKey;
import io.nats.service.ServiceMessage;
import io.nats.service.ServiceMessageHandler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.nats.jwt.JwtUtils.getClaimBody;

class AuthCalloutHandler implements ServiceMessageHandler {
    static String ISSUER_NSEED = "SAANDLKMXL6CUS3CP52WIXBEDN6YJ545GDKC65U5JZPPV6WH6ESWUA6YAI";

    static final Map<String, AuthCalloutTarget> NATS_AUTH_TARGETS;

    static final NKey USER_SIGNING_KEY;
    static final String PUB_USER_SIGNING_KEY;

    static {
        try {
            USER_SIGNING_KEY = NKey.fromSeed(ISSUER_NSEED.toCharArray());
            PUB_USER_SIGNING_KEY = new String(USER_SIGNING_KEY.getPublicKey());
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        // This sets up a map of users to simulate a back end auth system
        //   sys/sys, SYS
        //   alice/alice, APP
        //   token, APP
        //   bob/bob, APP, pub allow "bob.>", sub allow "bob.>", response max 1
        NATS_AUTH_TARGETS = new HashMap<>();
        NATS_AUTH_TARGETS.put("sys", new AuthCalloutTarget().userPass("sys").account("SYS"));
        NATS_AUTH_TARGETS.put("alice", new AuthCalloutTarget().userPass("alice").account("APP"));
        NATS_AUTH_TARGETS.put("token", new AuthCalloutTarget().authToken("token").account("APP"));
        Permission pb = new Permission().allow("bob.>");
        ResponsePermission r = new ResponsePermission().max(1);
        NATS_AUTH_TARGETS.put("bob", new AuthCalloutTarget().userPass("bob").account("APP").pub(pb).sub(pb).resp(r));
        NATS_AUTH_TARGETS.put("pub", new AuthCalloutTarget().userPass("pub").account("APP"));
        NATS_AUTH_TARGETS.put("reset", new AuthCalloutTarget().userPass("reset").account("APP"));
    }

    Connection nc;

    public AuthCalloutHandler(Connection nc) {
        this.nc = nc;
    }

    @Override
    public void onMessage(ServiceMessage smsg) {
        System.out.println("[HANDLER] Received Message");
        System.out.println("[HANDLER] Subject       : " + smsg.getSubject());
        System.out.println("[HANDLER] Headers       : " + headersToString(smsg.getHeaders()));

        try {
            // Convert the message data into a Claim
            Claim claim = new Claim(getClaimBody(smsg.getData()));
            System.out.println("[HANDLER] Claim-Request : " + claim.toJson());

            // The Claim should contain an Authorization Request
            AuthorizationRequest ar = claim.authorizationRequest;
            if (ar == null) {
                System.err.println("Invalid Authorization Request Claim");
                return;
            }
            printJson("[HANDLER] Auth Request  : ", ar.toJson(), "server_id", "user_nkey", "client_info", "connect_opts", "client_tls", "request_nonce");

            // Check if the user exists.
            AuthCalloutTarget acTarget;
            if (ar.connectOpts.authToken != null) {
                acTarget = NATS_AUTH_TARGETS.get(ar.connectOpts.authToken);
                if (acTarget == null) {
                    respond(smsg, ar, null, "Token Not Found: " + ar.connectOpts.authToken);
                    return;
                }
            }
            else {
                acTarget = NATS_AUTH_TARGETS.get(ar.connectOpts.user);
                if (acTarget == null) {
                    respond(smsg, ar, null, "User Not Found: " + ar.connectOpts.user);
                    return;
                }
                if (!acTarget.pass.equals(ar.connectOpts.pass)) {
                    respond(smsg, ar, null, "Password does not match: " + acTarget.pass + " != " + ar.connectOpts.pass);
                    return;
                }
            }

            UserClaim uc = new UserClaim()
                .pub(acTarget.pub)
                .sub(acTarget.sub)
                .resp(acTarget.resp);

            Duration expiresIn = null;
            if (ar.connectOpts.user != null) {
                if (ar.connectOpts.user.equals("reset")) {
                    flag = true;
                    System.out.println("[ DEBUG ] reset\n\n\n\n");
                }
                else if (ar.connectOpts.user.equals("alice") && flag) {
                    expiresIn = Duration.ofMillis(5000);
                    flag = false;
                    System.out.println("[ DEBUG ] AUTH ALICE 10 secs");
                }
                else {
                    System.out.println("[ DEBUG ] AUTH " + ar.connectOpts.user);
                }
            }

            String userJwt = new ClaimIssuer()
                .aud(acTarget.account)
                .name(ar.connectOpts.user)
                .iss(PUB_USER_SIGNING_KEY)
                .sub(ar.userNkey)
                .nats(uc)
                .expiresIn(expiresIn)
                .issueJwt(USER_SIGNING_KEY);

            respond(smsg, ar, userJwt, null);
        }
        catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
    }

    static boolean flag = true;
    private void respond(ServiceMessage smsg,
                         AuthorizationRequest ar,
                         String userJwt,
                         String error) throws GeneralSecurityException, IOException {

        AuthorizationResponse response = new AuthorizationResponse()
            .jwt(userJwt)
            .error(error);

        if (userJwt != null) {
            printJson("[HANDLER] Auth Resp JWT : ", getClaimBody(userJwt), "name", "nats");
        }
        else {
            System.out.println("[HANDLER] Auth Resp ERR : " + response.toJson());
        }

        String jwt = new ClaimIssuer()
            .aud(ar.serverId.id)
            .iss(PUB_USER_SIGNING_KEY)
            .sub(ar.userNkey)
            .nats(response)
            .issueJwt(USER_SIGNING_KEY);

        System.out.println("[HANDLER] Claim-Response: " + getClaimBody(jwt));
        smsg.respond(nc, jwt);
    }

    static final String SPACER = "                                                            ";
    private void printJson(String label, String json, String... splits) {
        if (splits != null && splits.length > 0) {
            String indent = SPACER.substring(0, label.length());
            boolean first = true;
            for (String split : splits) {
                int at = json.indexOf("\"" + split + "\"");
                if (at > 0) {
                    if (first) {
                        first = false;
                        System.out.println(label + json.substring(0, at));
                    }
                    else {
                        System.out.println(indent + json.substring(0, at));
                    }
                    json = json.substring(at);
                }
            }
            System.out.println(indent + json);
        }
        else {
            System.out.println(label + json);
        }
    }

    public static String headersToString(Headers h) {
        if (h == null || h.isEmpty()) {
            return "None";
        }
        boolean notFirst = false;
        StringBuilder sb = new StringBuilder("[");
        for (String key : h.keySet()) {
            if (notFirst) {
                sb.append(',');
            }
            else {
                notFirst = true;
            }
            sb.append(key).append("=").append(h.get(key));
        }
        return sb.append(']').toString();
    }
}
