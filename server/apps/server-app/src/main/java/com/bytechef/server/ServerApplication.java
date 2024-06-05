/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.server;

import com.bytechef.logback.config.CRLFLogConverter;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;

/**
 * @author Ivica Cardic
 */
@SpringBootApplication(scanBasePackages = "com.bytechef")
public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    private static UserService userService;

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ServerApplication.class);

        ApplicationContext applicationContext = springApplication.run(args);

        userService = applicationContext.getBean(UserService.class);

        Environment environment = applicationContext.getEnvironment();

        logApplicationStartup(environment);
    }

    private static void logApplicationStartup(Environment environment) {
        String protocol = Optional.ofNullable(environment.getProperty("server.ssl.key-store"))
            .map(key -> "https")
            .orElse("http");
        String serverPort = environment.getProperty("server.port");
        String contextPath = Optional.ofNullable(environment.getProperty("server.servlet.context-path"))
            .filter(StringUtils::isNotBlank)
            .orElse("/");
        String hostAddress = "localhost";

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();

            hostAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("The host name could not be determined, using `localhost` as fallback");
        }

        String[] environments = environment.getActiveProfiles();

        logger.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            """
                ----------------------------------------------------------
                \tApplication '{}' is running! Access URLs:
                \tLocal: \t\t{}://127.0.0.1:{}{}
                \tExternal: \t{}://{}:{}{}
                \tSwaggerUI: \t{}
                \tProfile(s): {}
                \tUsers: \t\t{}
                ----------------------------------------------------------""",
            environment.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            getSwaggerUiUrl(Arrays.asList(environments), protocol, serverPort, contextPath),
            environments,
            getUsers(Arrays.asList(environments)));
    }

    private static String getSwaggerUiUrl(
        List<String> environments, String protocol, String serverPort, String contextPath) {

        return environments.contains("api-docs")
            ? "%s://127.0.0.1:%s%s".formatted(protocol, serverPort, contextPath + "swagger-ui.html")
            : "";
    }

    private static String getUsers(List<String> environments) {
        return environments.contains("dev")
            ? userService.getAllActiveUsers(Pageable.ofSize(10))
                .stream()
                .map(User::getEmail)
                .collect(Collectors.joining(", "))
            : "";
    }
}
