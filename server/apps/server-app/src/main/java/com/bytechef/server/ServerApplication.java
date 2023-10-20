
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

package com.bytechef.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * @author Ivica Cardic
 */
@SpringBootApplication(scanBasePackages = "com.bytechef")
public class ServerApplication {

    private static final Logger log = LoggerFactory.getLogger(ServerApplication.class);

    private static String gitCommitId;

    @SuppressFBWarnings("ST")
    public ServerApplication(@Value("${git.commit.id}") String gitCommitId) {
        this.gitCommitId = gitCommitId;
    }

    /**
     * Main method, used to run the application.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ServerApplication.class);

        Environment environment = springApplication.run(args)
            .getEnvironment();

        logApplicationStartup(environment);
    }

    private static void logApplicationStartup(Environment environment) {
        String protocol = Optional.ofNullable(environment.getProperty("server.ssl.key-store"))
            .map(key -> "https")
            .orElse("http");
        String serverPort = environment.getProperty("server.port");
        String contextPath = Optional.ofNullable(environment.getProperty("server.servlet.context-path"))
            .filter(StringUtils::hasText)
            .orElse("/");
        String hostAddress = "localhost";

        try {
            InetAddress inetAddress = InetAddress.getLocalHost();

            hostAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }

        log.info(
            """
                ----------------------------------------------------------
                \tApplication '{}' is running! Access URLs:
                \tLocal: \t\t{}://localhost:{}{}
                \tExternal: \t{}://{}:{}{}
                \tSwaggerUI: \t{}
                \tProfile(s): \t{}
                \tGit Commit Id: \t{}
                ----------------------------------------------------------""",
            environment.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            Arrays.asList(environment.getActiveProfiles())
                .contains("api-docs")
                    ? "%s://localhost:%s%s".formatted(protocol, serverPort, contextPath + "swagger-ui.html")
                    : "",
            environment.getActiveProfiles(),
            gitCommitId);
    }
}
