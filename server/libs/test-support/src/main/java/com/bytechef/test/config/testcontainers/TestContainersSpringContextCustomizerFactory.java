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

package com.bytechef.test.config.testcontainers;

import com.bytechef.test.annotation.EmbeddedRedis;
import com.bytechef.test.annotation.EmbeddedSql;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

/**
 * @author Ivica Cardic
 */
public class TestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

    private static final Logger logger = LoggerFactory.getLogger(TestContainersSpringContextCustomizerFactory.class);

    private static PostgreSqlTestContainer postgreSqlTestContainer;
    private static RedisTestContainer redisTestContainer;

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return (context, mergedConfig) -> {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            TestPropertyValues testValues = TestPropertyValues.empty();

            List activeProfiles = Arrays.asList(context.getEnvironment().getActiveProfiles());

            if (activeProfiles.contains("testint")) {
                testValues = initPostgreSql(testClass, beanFactory, testValues);

                testValues = initRedis(testClass, beanFactory, testValues);
            }

            testValues.applyTo(context);
        };
    }

    private static TestPropertyValues initRedis(
            Class<?> testClass, ConfigurableListableBeanFactory beanFactory, TestPropertyValues testValues) {
        EmbeddedRedis embeddedRedis = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedRedis.class);

        if (null != embeddedRedis) {
            logger.debug("Detected the EmbeddedRedis annotation on class {}.", testClass.getName());
            logger.info("Warming up the Redis instance.");

            if (null == redisTestContainer) {
                Class<RedisTestContainer> containerClass = RedisTestContainer.class;

                redisTestContainer = beanFactory.createBean(containerClass);

                beanFactory.registerSingleton(containerClass.getName(), redisTestContainer);
            }

            testValues = testValues.and(
                    "spring.redis.host=" + redisTestContainer.getTestContainer().getHost());
            testValues = testValues.and(
                    "spring.redis.port=" + redisTestContainer.getTestContainer().getMappedPort(6379));
        }

        return testValues;
    }

    private static TestPropertyValues initPostgreSql(
            Class<?> testClass, ConfigurableListableBeanFactory beanFactory, TestPropertyValues testValues) {
        EmbeddedSql embeddedSql = AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedSql.class);

        if (null != embeddedSql) {
            logger.debug("Detected the EmbeddedSql annotation on class {}.", testClass.getName());
            logger.info("Warming up the PostgreSql database.");

            if (null == postgreSqlTestContainer) {
                Class<PostgreSqlTestContainer> containerClass = PostgreSqlTestContainer.class;

                postgreSqlTestContainer = beanFactory.createBean(containerClass);

                beanFactory.registerSingleton(containerClass.getName(), postgreSqlTestContainer);
            }

            testValues = testValues.and("spring.datasource.url="
                    + postgreSqlTestContainer.getTestContainer().getJdbcUrl());
            testValues = testValues.and("spring.datasource.username="
                    + postgreSqlTestContainer.getTestContainer().getUsername());
            testValues = testValues.and("spring.datasource.password="
                    + postgreSqlTestContainer.getTestContainer().getPassword());
        }

        return testValues;
    }
}
