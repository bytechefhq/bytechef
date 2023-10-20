
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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import reactor.core.publisher.Flux;

/**
 * @author Ivica Cardic
 */
public class RedisReactiveDiscoveryClient implements ReactiveDiscoveryClient {

    private final RedisDiscoveryClient redisDiscoveryClient;

    public RedisReactiveDiscoveryClient(RedisTemplate<String, RedisRegistration> redisTemplate) {
        this.redisDiscoveryClient = new RedisDiscoveryClient(redisTemplate);
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public Flux<ServiceInstance> getInstances(String serviceId) {
        return Flux.fromIterable(redisDiscoveryClient.getInstances(serviceId));
    }

    @Override
    public Flux<String> getServices() {
        return Flux.fromIterable(redisDiscoveryClient.getServices());
    }
}
