
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.discovery.redis.metadata;

import com.bytechef.discovery.metadata.ServiceMetadataRegistry;
import com.bytechef.discovery.redis.registry.RedisRegistration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class RedisServiceMetadataRegistry implements ServiceMetadataRegistry {

    private final RedisRegistration redisRegistration;

    @SuppressFBWarnings("EI2")
    public RedisServiceMetadataRegistry(RedisRegistration redisRegistration) {
        this.redisRegistration = redisRegistration;
    }

    @Override
    public void registerMetadata(Map<String, String> metadata) {
        Map<String, String> curMetadataMap = redisRegistration.getMetadata();

        Set<Map.Entry<String, String>> curMetadataEntry = curMetadataMap.entrySet();
        Set<Map.Entry<String, String>> metadataEntry = metadata.entrySet();

        redisRegistration.setMetadata(
            Stream.concat(curMetadataEntry.stream(), metadataEntry.stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
