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

package com.bytechef.commons.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Igor Beslic
 */
public class ObfuscateUtilsTest {

    @Test
    public void testObfuscate() {
        assertThat(ObfuscateUtils.obfuscate(null, 10, 3))
            .isNull();

        assertThat(ObfuscateUtils.obfuscate("", 10, 3))
            .isEmpty();

        assertThat(ObfuscateUtils.obfuscate("12345678", 10, 3))
            .isEqualTo("..........678");

        assertThat(ObfuscateUtils.obfuscate("123456789012", 10, 3))
            .isEqualTo("..........012");

        assertThat(ObfuscateUtils.obfuscate("12345678", 10, 15))
            .isEqualTo("..........12345678");
    }

    @Test
    public void testToObfuscatedMap() {
        Map<String, Object> map = createParametersMap();

        Map<String, Object> obfuscatedMap = ObfuscateUtils.toObfuscatedMap(map, 5, 2);

        assertThat(obfuscatedMap.get("addTo"))
            .isEqualTo("to-value");

        assertThat(obfuscatedMap.get("authorizationType"))
            .isEqualTo("auth-type");

        assertThat(obfuscatedMap.get("authorizationUrl"))
            .isEqualTo("https://example.com/auth");

        assertThat(obfuscatedMap.get("base"))
            .isEqualTo("base-value");

        assertThat(obfuscatedMap.get("baseUri"))
            .isEqualTo("base-uri");

        assertThat(obfuscatedMap.get("baseUrl"))
            .isEqualTo("base-url");

        assertThat(obfuscatedMap.get("bucketName"))
            .isEqualTo("project-mandragora");

        assertThat(obfuscatedMap.get("clientId"))
            .isEqualTo("client-id-123");

        assertThat(obfuscatedMap.get("collection"))
            .isEqualTo("collection-value");

        assertThat(obfuscatedMap.get("collectionName"))
            .isEqualTo("collection-name");

        assertThat(obfuscatedMap.get("companyDomain"))
            .isEqualTo("company-domain");

        assertThat(obfuscatedMap.get("companyId"))
            .isEqualTo("company-id");

        assertThat(obfuscatedMap.get("connectionString"))
            .isEqualTo("connection-string");

        assertThat(obfuscatedMap.get("containerName"))
            .isEqualTo("container-name");

        assertThat(obfuscatedMap.get("contactPoints"))
            .isEqualTo("contact-points");

        assertThat(obfuscatedMap.get("cryptoProtocol"))
            .isEqualTo("crypto-protocol");

        assertThat(obfuscatedMap.get("database"))
            .isEqualTo("database-value");

        assertThat(obfuscatedMap.get("databaseName"))
            .isEqualTo("database-name");

        assertThat(obfuscatedMap.get("datacenter"))
            .isEqualTo("datacenter-value");

        assertThat(obfuscatedMap.get("deployment"))
            .isEqualTo("deployment-value");

        assertThat(obfuscatedMap.get("dimensions"))
            .isEqualTo("dimensions-value");

        assertThat(obfuscatedMap.get("distanceType"))
            .isEqualTo("distance-type");

        assertThat(obfuscatedMap.get("domain"))
            .isEqualTo("domain-value");

        assertThat(obfuscatedMap.get("embeddingDimension"))
            .isEqualTo("embedding-dimension");

        assertThat(obfuscatedMap.get("endpoint"))
            .isEqualTo("endpoint-value");

        assertThat(obfuscatedMap.get("headerPrefix"))
            .isEqualTo("header-prefix");

        assertThat(obfuscatedMap.get("host"))
            .isEqualTo("localhost");

        assertThat(obfuscatedMap.get("indexName"))
            .isEqualTo("index-name");

        assertThat(obfuscatedMap.get("initializeSchema"))
            .isEqualTo("initialize-schema");

        assertThat(obfuscatedMap.get("instance_url"))
            .isEqualTo("instance-url");

        assertThat(obfuscatedMap.get("keyPrefix"))
            .isEqualTo("key-prefix");

        assertThat(obfuscatedMap.get("keyspace"))
            .isEqualTo("keyspace-value");

        assertThat(obfuscatedMap.get("optimization"))
            .isEqualTo("optimization-value");

        assertThat(obfuscatedMap.get("organization_id"))
            .isEqualTo("organization-id");

        assertThat(obfuscatedMap.get("port"))
            .isEqualTo("8080");

        assertThat(obfuscatedMap.get("protocol"))
            .isEqualTo("protocol-value");

        assertThat(obfuscatedMap.get("publicEndpoint"))
            .isEqualTo("public-endpoint");

        assertThat(obfuscatedMap.get("refreshUrl"))
            .isEqualTo("refresh-url");

        assertThat(obfuscatedMap.get("region"))
            .isEqualTo("eu-ireland");

        assertThat(obfuscatedMap.get("schemaName"))
            .isEqualTo("schema-name");

        assertThat(obfuscatedMap.get("scopeName"))
            .isEqualTo("scope-name");

        assertThat(obfuscatedMap.get("scopes"))
            .isEqualTo("scopes-value");

        assertThat(obfuscatedMap.get("serverUrl"))
            .isEqualTo("server-url");

        assertThat(obfuscatedMap.get("shopName"))
            .isEqualTo("shop-name");

        assertThat(obfuscatedMap.get("similarity"))
            .isEqualTo("similarity-value");

        assertThat(obfuscatedMap.get("siteName"))
            .isEqualTo("site-name");

        assertThat(obfuscatedMap.get("subdomain"))
            .isEqualTo("subdomain-value");

        assertThat(obfuscatedMap.get("table"))
            .isEqualTo("table-value");

        assertThat(obfuscatedMap.get("tableName"))
            .isEqualTo("table-name");

        assertThat(obfuscatedMap.get("timeToLive"))
            .isEqualTo("time-to-live");

        assertThat(obfuscatedMap.get("tokenUrl"))
            .isEqualTo("token-url");

        assertThat(obfuscatedMap.get("uri"))
            .isEqualTo("uri-value");

        assertThat(obfuscatedMap.get("url"))
            .isEqualTo("https://api.example.com");

        assertThat(obfuscatedMap.get("username"))
            .isEqualTo("user-789");

        assertThat(obfuscatedMap.get("website"))
            .isEqualTo("website-value");

        assertThat(obfuscatedMap.get("password"))
            .isEqualTo(".....");

        assertThat(obfuscatedMap.get("apiKey"))
            .isEqualTo(".....90");

        assertThat(obfuscatedMap.get("token"))
            .isEqualTo(".....AB");

        assertThat(obfuscatedMap.get("clientSecret"))
            .isEqualTo(".....56");

    }

    private static Map<String, Object> createParametersMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("addTo", "to-value");
        map.put("authorizationType", "auth-type");
        map.put("authorizationUrl", "https://example.com/auth");
        map.put("base", "base-value");
        map.put("baseUri", "base-uri");
        map.put("baseUrl", "base-url");
        map.put("bucketName", "project-mandragora");
        map.put("clientId", "client-id-123");
        map.put("collection", "collection-value");
        map.put("collectionName", "collection-name");
        map.put("companyDomain", "company-domain");
        map.put("companyId", "company-id");
        map.put("connectionString", "connection-string");
        map.put("containerName", "container-name");
        map.put("contactPoints", "contact-points");
        map.put("cryptoProtocol", "crypto-protocol");
        map.put("database", "database-value");
        map.put("databaseName", "database-name");
        map.put("datacenter", "datacenter-value");
        map.put("deployment", "deployment-value");
        map.put("dimensions", "dimensions-value");
        map.put("distanceType", "distance-type");
        map.put("domain", "domain-value");
        map.put("embeddingDimension", "embedding-dimension");
        map.put("endpoint", "endpoint-value");
        map.put("headerPrefix", "header-prefix");
        map.put("host", "localhost");
        map.put("indexName", "index-name");
        map.put("initializeSchema", "initialize-schema");
        map.put("instance_url", "instance-url");
        map.put("keyPrefix", "key-prefix");
        map.put("keyspace", "keyspace-value");
        map.put("optimization", "optimization-value");
        map.put("organization_id", "organization-id");
        map.put("port", 8080);
        map.put("protocol", "protocol-value");
        map.put("publicEndpoint", "public-endpoint");
        map.put("refreshUrl", "refresh-url");
        map.put("region", "eu-ireland");
        map.put("schemaName", "schema-name");
        map.put("scopeName", "scope-name");
        map.put("scopes", "scopes-value");
        map.put("serverUrl", "server-url");
        map.put("shopName", "shop-name");
        map.put("similarity", "similarity-value");
        map.put("siteName", "site-name");
        map.put("subdomain", "subdomain-value");
        map.put("table", "table-value");
        map.put("tableName", "table-name");
        map.put("timeToLive", "time-to-live");
        map.put("tokenUrl", "token-url");
        map.put("uri", "uri-value");
        map.put("url", "https://api.example.com");
        map.put("username", "user-789");
        map.put("website", "website-value");
        map.put("password", "secret123");
        map.put("apiKey", "1234567890");
        map.put("token", "AB048-F4E5A-00234-0045AB");
        map.put("clientSecret", "test-secret");

        return map;
    }
}
