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

package com.bytechef.component.liferay.constant;

/**
 * @author Nikolina Spehar
 */
public enum LiferayContextName {

    PORTAL("portal"),
    ACCOUNT("account"),
    ASSET("asset"),
    ASSETLIST("assetlist"),
    AUDIT("audit"),
    BACKGROUNDTASK("backgroundtask"),
    BATCHENGINE("batchengine"),
    BLOGS("blogs"),
    CALENDAR("calendar"),
    COMMENT("comment"),
    COMMERCE("commerce"),
    CONTACT("contact"),
    CT("ct"),
    DDL("ddl"),
    DDM("ddm"),
    DEPOT("depot"),
    DISPATCH("dispatch"),
    FRAGMENT("fragment"),
    JOURNAL("journal"),
    KALEO("kaleo"),
    KALEOFORMS("kaleoforms"),
    KB("kb"),
    LAYOUT("layout"),
    LAYOUTUTILITYPAGE("layoututilitypage"),
    LISTTYPE("listtype"),
    MARKETPLACE("marketplace"),
    MB("mb"),
    NOTIFICATION("notification"),
    OAUTHCLIENT("oauthclient"),
    OBJECT("object"),
    PORTALLANGUAGEOVERRRIDE("portallanguageoverride"),
    REDIRECT("redirect"),
    REMOTEAPP("remoteapp"),
    SAP("sap"),
    SAVEDCONTENTENTRY("savedcontententry"),
    SEGMENTS("segments"),
    SHARING("sharing"),
    SITENAVIGATION("sitenavigation"),
    STYLEBOOK("stylebook"),
    SXP("sxp"),
    TRANSLATION("translation"),
    TRASH("trash"),
    WIKI("wiki");

    private final String contextName;

    LiferayContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getContextName() {
        return this.contextName;
    }
}
