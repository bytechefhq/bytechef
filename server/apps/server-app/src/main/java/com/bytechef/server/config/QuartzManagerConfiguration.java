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

package com.bytechef.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Exposes the Quartz Manager dashboard while developing locally.
 *
 * <p>
 * The Quartz Manager starters are declared as {@code developmentOnly} dependencies, so they are on the {@code bootRun}
 * classpath but never packaged into the application jar. Nothing here refers to a Quartz Manager type, which keeps the
 * configuration loadable even when the starters are absent.
 *
 * <p>
 * The scheduler alias is registered whenever the application runs on Quartz, because the starters are on the
 * {@code bootRun} classpath no matter which profile is active and Quartz Manager fails to start without it. The alias
 * is inert when the starters are absent. Reaching the dashboard is what stays limited to the development profile: it
 * operates on the scheduler that runs the workflow triggers, and it can pause, reschedule and delete them.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef", name = "scheduler.provider", havingValue = "quartz", matchIfMissing = true)
@Import(QuartzManagerSchedulerRegistrar.class)
public class QuartzManagerConfiguration {

    /**
     * Opens up the Quartz Manager REST API, its STOMP endpoints and the dashboard resources.
     *
     * <p>
     * The chain is ordered ahead of the application chains because the last of them denies every unmatched request. The
     * dashboard is opened directly on the server port, so it carries no session established through the client
     * development server, and authentication is left off rather than made unusable.
     *
     * @param http the {@link HttpSecurity} object used to customize security settings for the Quartz Manager endpoints
     * @return a configured {@link SecurityFilterChain} serving the Quartz Manager API and dashboard
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
    @Order(0)
    @Profile("dev")
    SecurityFilterChain quartzManagerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/quartz-manager/**", "/quartz-manager-ui/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .anyRequest()
                .permitAll());

        return http.build();
    }

    /**
     * Registers the Quartz Manager beans under names of our own choosing.
     *
     * <p>
     * This repeats the scan that {@code QuartzManagerApiConfig} performs, which is why that class is named in
     * {@code spring.autoconfigure.exclude} and filtered out below: left to run, it would scan the same package again
     * under the default names and reintroduce the {@code jobService} clash that {@link QuartzManagerBeanNameGenerator}
     * exists to avoid.
     *
     * @author Ivica Cardic
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "it.fabioformosa.quartzmanager.api.services.JobService")
    @ComponentScan(
        basePackages = "it.fabioformosa.quartzmanager.api",
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "it\\.fabioformosa\\.quartzmanager\\.api\\.configuration\\.QuartzManagerApiConfig"),
        nameGenerator = QuartzManagerBeanNameGenerator.class)
    static class QuartzManagerComponentScanConfiguration {

        /**
         * Repairs the conversion service of the whole context after Quartz Manager replaces it.
         *
         * @return the {@link QuartzManagerConversionServiceBeanPostProcessor}
         */
        @Bean
        static QuartzManagerConversionServiceBeanPostProcessor quartzManagerConversionServiceBeanPostProcessor() {
            return new QuartzManagerConversionServiceBeanPostProcessor();
        }
    }
}
