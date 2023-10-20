
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.discovery.redis.registry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ivica Cardic
 */
public class RedisServiceRegistry implements ServiceRegistry<RedisRegistration> {

    private static final Logger logger = LoggerFactory.getLogger(RedisServiceRegistry.class);

    private RedisRegistration redisRegistration;
    private final RedisTemplate<String, RedisRegistration> redisTemplate;
    private boolean stopped;

    @SuppressFBWarnings("EI2")
    public RedisServiceRegistry(
        RedisTemplate<String, RedisRegistration> redisTemplate, TaskExecutor taskExecutor) {
        this.redisTemplate = redisTemplate;

        taskExecutor.execute(this::periodicallyRegisterService);
    }

    @Override
    @SuppressFBWarnings("EI2")
    public void register(RedisRegistration redisRegistration) {
        registerService(redisRegistration);

        this.redisRegistration = redisRegistration;
    }

    @Override
    public void deregister(RedisRegistration redisRegistration) {
        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        listOperations.remove(redisRegistration.getServiceId(), 1, redisRegistration);

        this.redisRegistration = null;
    }

    @Override
    public void close() {
        stopped = true;

        logger.info("Redis Service Registry is closed");
    }

    @Override
    public void setStatus(RedisRegistration registration, String status) {
    }

    @Override
    public <T> T getStatus(RedisRegistration registration) {
        return null;
    }

    private void periodicallyRegisterService() {
        while (!stopped) {
            try {
                if (redisRegistration == null) {
                    TimeUnit.SECONDS.sleep(1);

                    continue;
                }

                registerService(redisRegistration);

                TimeUnit.SECONDS.sleep(10);
            } catch (Exception e) {
                if (!stopped) {
                    logger.error(e.getMessage(), e);

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    @SuppressFBWarnings("RCN")
    private void registerService(RedisRegistration redisRegistration) {
        String serviceId = "discovery/" + redisRegistration.getServiceId();

        ListOperations<String, RedisRegistration> listOperations = redisTemplate.opsForList();

        Long index = listOperations.indexOf(serviceId, redisRegistration);

        if (index == null) {
            listOperations.leftPush(serviceId, redisRegistration);
        }

        redisTemplate.expire(serviceId, 15, TimeUnit.SECONDS);
    }
}
