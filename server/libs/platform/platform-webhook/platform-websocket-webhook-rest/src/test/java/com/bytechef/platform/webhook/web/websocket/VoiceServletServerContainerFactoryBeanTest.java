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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class VoiceServletServerContainerFactoryBeanTest {

    @Test
    void testSkipsConfigurationWhenTheServletContextHasNoWebSocketContainer() {
        ServletContext servletContext = mock(ServletContext.class);
        VoiceServletServerContainerFactoryBean factoryBean = new VoiceServletServerContainerFactoryBean();

        factoryBean.setServletContext(servletContext);

        assertThatCode(factoryBean::afterPropertiesSet).doesNotThrowAnyException();
        assertThat(factoryBean.getObject()).isNull();
    }

    @Test
    void testConfiguresTheWebSocketContainerWhenTheServletContextHasOne() {
        ServerContainer serverContainer = mock(ServerContainer.class);
        ServletContext servletContext = mock(ServletContext.class);

        when(servletContext.getAttribute(VoiceServletServerContainerFactoryBean.SERVER_CONTAINER_ATTRIBUTE))
            .thenReturn(serverContainer);

        VoiceServletServerContainerFactoryBean factoryBean = new VoiceServletServerContainerFactoryBean();

        factoryBean.setMaxBinaryMessageBufferSize(65536);
        factoryBean.setServletContext(servletContext);

        factoryBean.afterPropertiesSet();

        assertThat(factoryBean.getObject()).isSameAs(serverContainer);

        verify(serverContainer).setDefaultMaxBinaryMessageBufferSize(65536);
    }
}
