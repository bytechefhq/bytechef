/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.rocketchat.constant;

import static com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

/**
 * @author Marija Horvat
 */
public class RocketchatConstants {

    public static final String DOMAIN = "domain";
    public static final String EXCLUDE_SELF = "excludeSelf";
    public static final String ID = "id";
    public static final String MEMBERS = "members";
    public static final String NAME = "name";
    public static final String READ_ONLY = "readOnly";
    public static final String ROOM_ID = "roomId";
    public static final String TEXT = "text";
    public static final String USERNAME = "username";
    public static final String X_AUTH_TOKEN = "X-Auth-Token";
    public static final String X_USER_ID = "X-User-Id";

    public static final ModifiableObjectProperty POST_MESSAGE_RESPONSE_PROPERTY = object()
        .properties(
            integer("ts"),
            string("channel"),
            object("message")
                .properties(
                    string("alias"),
                    string("msg"),
                    array("attachments"),
                    bool("parseUrls"),
                    bool("groupable"),
                    string("ts"),
                    object("u")
                        .properties(
                            string("_id"),
                            string("username"),
                            string("name")),
                    string("rid"),
                    string("_id"),
                    string("_updateAt"),
                    array("urls"),
                    array("mentions"),
                    array("channels"),
                    array("md")),
            bool("success"));

    public static final ModifiableObjectProperty CHANNEL_RESPONSE_PROPERTY = object()
        .properties(
            object("channel")
                .properties(
                    string("_id"),
                    string("fname"),
                    string("_updateAt"),
                    object("customFields"),
                    string("name"),
                    string("t"),
                    integer("msgs"),
                    integer("usersCount"),
                    object("u")
                        .properties(
                            string("_id"),
                            string("username"),
                            string("name")),
                    string("ts"),
                    bool("ro"),
                    bool("default"),
                    bool("sysMes")),
            bool("success"));

    private RocketchatConstants() {
    }
}
