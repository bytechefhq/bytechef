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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    public void testToObfuscatedMapPreservesInsensitiveValues() {
        Map<String, Object> map = createParametersMap();

        Map<String, Object> obfuscatedMap = ObfuscateUtils.toObfuscatedMap(map, 5, 2);

        assertThat(obfuscatedMap)
            .containsEntry("addTo", "to-value")
            .containsEntry("authorizationType", "auth-type")
            .containsEntry("authorizationUrl", "https://example.com/auth")
            .containsEntry("base", "base-value")
            .containsEntry("baseUri", "base-uri")
            .containsEntry("baseUrl", "base-url")
            .containsEntry("bucketName", "project-mandragora")
            .containsEntry("clientId", "client-id-123")
            .containsEntry("collection", "collection-value")
            .containsEntry("collectionName", "collection-name")
            .containsEntry("companyDomain", "company-domain")
            .containsEntry("companyId", "company-id")
            .containsEntry("connectionString", "connection-string")
            .containsEntry("containerName", "container-name")
            .containsEntry("contactPoints", "contact-points")
            .containsEntry("cryptoProtocol", "crypto-protocol")
            .containsEntry("database", "database-value")
            .containsEntry("databaseName", "database-name")
            .containsEntry("datacenter", "datacenter-value")
            .containsEntry("deployment", "deployment-value")
            .containsEntry("dimensions", "dimensions-value")
            .containsEntry("distanceType", "distance-type")
            .containsEntry("domain", "domain-value")
            .containsEntry("embeddingDimension", "embedding-dimension")
            .containsEntry("endpoint", "endpoint-value")
            .containsEntry("headerPrefix", "header-prefix")
            .containsEntry("host", "localhost")
            .containsEntry("indexName", "index-name")
            .containsEntry("initializeSchema", "initialize-schema")
            .containsEntry("instance_url", "instance-url")
            .containsEntry("keyPrefix", "key-prefix")
            .containsEntry("keyspace", "keyspace-value")
            .containsEntry("optimization", "optimization-value")
            .containsEntry("organization_id", "organization-id")
            .containsEntry("port", "8080")
            .containsEntry("protocol", "protocol-value")
            .containsEntry("publicEndpoint", "public-endpoint")
            .containsEntry("refreshUrl", "refresh-url")
            .containsEntry("region", "eu-ireland")
            .containsEntry("schemaName", "schema-name")
            .containsEntry("scopeName", "scope-name")
            .containsEntry("scopes", "scopes-value")
            .containsEntry("serverUrl", "server-url")
            .containsEntry("shopName", "shop-name")
            .containsEntry("similarity", "similarity-value")
            .containsEntry("siteName", "site-name")
            .containsEntry("subdomain", "subdomain-value")
            .containsEntry("table", "table-value")
            .containsEntry("tableName", "table-name")
            .containsEntry("timeToLive", "time-to-live")
            .containsEntry("tokenUrl", "token-url")
            .containsEntry("uri", "uri-value")
            .containsEntry("url", "https://api.example.com")
            .containsEntry("username", "user-789")
            .containsEntry("website", "website-value");
    }

    @Test
    public void testToObfuscatedMapObfuscatesSensitiveValues() {
        Map<String, Object> map = createParametersMap();

        Map<String, Object> obfuscatedMap = ObfuscateUtils.toObfuscatedMap(map, 5, 2);

        assertThat(obfuscatedMap)
            .containsEntry("password", ".....")
            .containsEntry("apiKey", ".....90")
            .containsEntry("token", ".....AB")
            .containsEntry("clientSecret", ".....et");
    }

    @SuppressFBWarnings("HARD_CODE_PASSWORD")
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
