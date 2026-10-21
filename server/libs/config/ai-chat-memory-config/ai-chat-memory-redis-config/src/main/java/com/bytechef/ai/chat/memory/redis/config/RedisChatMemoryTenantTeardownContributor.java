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

package com.bytechef.ai.chat.memory.redis.config;

import com.bytechef.tenant.teardown.TenantTeardownContributor;
import com.bytechef.tenant.util.TenantIdUtils;
import java.util.List;
import org.springframework.core.annotation.Order;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * @author Ivica Cardic
 */
@Order(1200)
class RedisChatMemoryTenantTeardownContributor implements TenantTeardownContributor {

    private static final int SCAN_COUNT = 500;

    private final RedisClient redisClient;

    RedisChatMemoryTenantTeardownContributor(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    public void afterSchemaDrop(String tenantId) {
        TenantIdUtils.requireValid(tenantId);

        dropIndex(TenantRoutingRedisChatMemoryRepository.getIndexName(tenantId));
        deleteKeys(TenantRoutingRedisChatMemoryRepository.getKeyPrefix(tenantId));
    }

    private void dropIndex(String indexName) {
        try {
            redisClient.ftDropIndex(indexName);
        } catch (JedisDataException jedisDataException) {
            String message = jedisDataException.getMessage();

            if (message == null || !message.toLowerCase()
                .contains("unknown")) {

                throw jedisDataException;
            }
        }
    }

    private void deleteKeys(String keyPrefix) {
        ScanParams scanParams = new ScanParams().match(keyPrefix + "*")
            .count(SCAN_COUNT);

        String cursor = ScanParams.SCAN_POINTER_START;

        do {
            ScanResult<String> scanResult = redisClient.scan(cursor, scanParams);

            List<String> keys = scanResult.getResult();

            if (!keys.isEmpty()) {
                redisClient.unlink(keys.toArray(new String[0]));
            }

            cursor = scanResult.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
    }
}
