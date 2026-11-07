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

package com.bytechef.platform.webhook.web.websocket;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.ServletContext;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @author Ivica Cardic
 */
class VoiceServletServerContainerFactoryBean extends ServletServerContainerFactoryBean {

    static final String SERVER_CONTAINER_ATTRIBUTE = "jakarta.websocket.server.ServerContainer";

    private ServletContext servletContext;

    @Override
    @SuppressFBWarnings("EI")
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;

        super.setServletContext(servletContext);
    }

    @Override
    public void afterPropertiesSet() {
        if (servletContext == null || servletContext.getAttribute(SERVER_CONTAINER_ATTRIBUTE) == null) {
            return;
        }

        super.afterPropertiesSet();
    }
}
