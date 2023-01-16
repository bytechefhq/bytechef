
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

package com.bytechef.discovery.redis.client;

import com.bytechef.discovery.redis.registry.RedisRegistration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author Ivica Cardic
 */
public class RedisDiscoveryClient implements DiscoveryClient {

    private final RedisTemplate<String, RedisRegistration> redisTemplate;

    @SuppressFBWarnings("EI2")
    public RedisDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String description() {
        return "Redis Discovery Client";
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<ServiceInstance> getInstances(String serviceId) {
        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        List<RedisRegistration> redisRegistrations = listOperations.range(serviceId, 0, -1);

        return Objects.requireNonNull(redisRegistrations)
            .parallelStream()
            .collect(Collectors.toList());
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<String> getServices() {
        return new ArrayList<>(Objects.requireNonNull(redisTemplate.keys("*")));
    }
}
