/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.session.config.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.bytechef.ee.session.config.boot.SessionStoreEnvironmentPostProcessor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.ClassUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Hooks;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class RedisSessionConnectionAutoConfigurationIntTest {

    private static final String AUTOCONFIGURE_EXCLUDE = String.join(
        ",", "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
        "org.springframework.boot.session.data.redis.autoconfigure.SessionDataRedisAutoConfiguration",
        "org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration");

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine").withExposedPorts(6379);

    private static final String SECURITY_REACTOR_CONTEXT_SUBSCRIBER_REGISTRAR =
        "org.springframework.security.config.annotation.web.configuration.SecurityReactorContextConfiguration"
            + "$SecurityReactorContextSubscriberRegistrar";

    private Object securityReactorContextSubscriberRegistrar;

    @BeforeEach
    void beforeEach() throws Exception {
        Hooks.enableAutomaticContextPropagation();

        securityReactorContextSubscriberRegistrar = BeanUtils.instantiateClass(
            ClassUtils.forName(SECURITY_REACTOR_CONTEXT_SUBSCRIBER_REGISTRAR, null));

        ((InitializingBean) securityReactorContextSubscriberRegistrar).afterPropertiesSet();
    }

    @AfterEach
    void afterEach() throws Exception {
        ((DisposableBean) securityReactorContextSubscriberRegistrar).destroy();

        Hooks.disableAutomaticContextPropagation();

        SecurityContextHolder.clearContext();
    }

    @Test
    void testFirstRequestWithSessionCookieOnFreshNodeLoadsSecurityContext() {
        WebApplicationContextRunner webApplicationContextRunner = createRunner();

        webApplicationContextRunner.run(nodeA -> {
            SessionRepository<Session> nodeARepository = getSessionRepository(nodeA);

            Session session = nodeARepository.createSession();

            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(
                    UsernamePasswordAuthenticationToken.authenticated("admin@localhost.com", null, List.of())));

            nodeARepository.save(session);

            webApplicationContextRunner.run(nodeB -> {
                AtomicReference<String> userName = new AtomicReference<>();

                assertTimeoutPreemptively(
                    Duration.ofSeconds(30), () -> serveRequest(nodeB, session.getId(), userName));

                assertThat(userName.get()).isEqualTo("admin@localhost.com");
            });
        });
    }

    private static WebApplicationContextRunner createRunner() {
        return new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    DataRedisAutoConfiguration.class, RedisSessionConnectionAutoConfiguration.class,
                    SessionAutoConfiguration.class, SessionDataRedisAutoConfiguration.class))
            .withInitializer(
                context -> new SessionStoreEnvironmentPostProcessor().postProcessEnvironment(
                    context.getEnvironment(), new SpringApplication()))
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.session.store=redis",
                "spring.autoconfigure.exclude=" + AUTOCONFIGURE_EXCLUDE,
                "spring.data.redis.host=" + redis.getHost(),
                "spring.data.redis.port=" + redis.getMappedPort(6379));
    }

    @SuppressWarnings("unchecked")
    private static SessionRepository<Session> getSessionRepository(AssertableWebApplicationContext context) {
        return context.getBean(SessionRepository.class);
    }

    private static void serveRequest(
        AssertableWebApplicationContext context, String sessionId, AtomicReference<String> userName)
        throws Exception {

        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();

        cookieSerializer.setUseBase64Encoding(false);

        CookieHttpSessionIdResolver cookieHttpSessionIdResolver = new CookieHttpSessionIdResolver();

        cookieHttpSessionIdResolver.setCookieSerializer(cookieSerializer);

        SessionRepositoryFilter<Session> sessionRepositoryFilter =
            new SessionRepositoryFilter<>(getSessionRepository(context));

        sessionRepositoryFilter.setHttpSessionIdResolver(cookieHttpSessionIdResolver);

        HttpServlet servlet = new HttpServlet() {

            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) {
                Authentication authentication = SecurityContextHolder.getContext()
                    .getAuthentication();

                userName.set(authentication == null ? null : authentication.getName());
            }
        };

        MockFilterChain mockFilterChain = new MockFilterChain(
            servlet, sessionRepositoryFilter,
            new SecurityContextHolderFilter(new HttpSessionSecurityContextRepository()));

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("GET", "/api/account");

        mockHttpServletRequest.setCookies(new Cookie("SESSION", sessionId));

        mockFilterChain.doFilter(mockHttpServletRequest, new MockHttpServletResponse());
    }
}
