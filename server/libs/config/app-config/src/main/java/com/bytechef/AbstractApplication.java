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

package com.bytechef;

import com.bytechef.logback.config.CRLFLogConverter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractApplication {

    private static final Logger logger = LoggerFactory.getLogger(AbstractApplication.class);

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();

        logApplicationStartup(applicationContext.getEnvironment());
    }

    private static void logApplicationStartup(Environment environment) {
        String protocol = Optional.ofNullable(environment.getProperty("server.ssl.key-store"))
            .map(key -> "https")
            .orElse("http");
        String serverPort = environment.getProperty("server.port");
        String contextPath = Optional.ofNullable(environment.getProperty("server.servlet.context-path"))
            .filter(StringUtils::isNotBlank)
            .orElse("/");

        String[] activeProfiles = environment.getActiveProfiles();

        logger.info(
            CRLFLogConverter.CRLF_SAFE_MARKER,
            """
                \n----------------------------------------------------------
                \tApplication '{}' is running! Access URLs:
                \tLocal: \t\t{}://127.0.0.1:{}{}
                \tExternal: \t{}://{}:{}{}
                \tEdition: \t{}
                \tTenant mode: {}
                \tProfile(s): {}
                \tSwaggerUI: \t{}
                ----------------------------------------------------------""",
            environment.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            getHostAddress(),
            serverPort,
            contextPath,
            StringUtils.upperCase(environment.getProperty("bytechef.edition")),
            environment.getProperty("bytechef.tenant.mode"),
            activeProfiles,
            getSwaggerUiUrl(Arrays.asList(activeProfiles), protocol, serverPort, contextPath));
    }

    private static String getHostAddress() {
        String hostAddress = "localhost";

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();

            hostAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("The host name could not be determined, using `localhost` as fallback");
        }
        return hostAddress;
    }

    private static String getSwaggerUiUrl(
        List<String> activeProfiles, String protocol, String serverPort, String contextPath) {

        return activeProfiles.contains("api-docs")
            ? "%s://127.0.0.1:%s%s".formatted(protocol, serverPort, contextPath + "swagger-ui.html")
            : "-";
    }
}
