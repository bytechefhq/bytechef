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

package com.bytechef.platform.user.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class TempEmailServiceImpl implements TempEmailService, InitializingBean {

    private final Resource resource;
    private List<String> tempEmailDomains;

    public TempEmailServiceImpl(@Value("${bytechef.security.email.temp-domains-url:''}") Resource resource) {
        this.resource = resource;
    }

    @Override
    public boolean isEmailTemp(String email) {
        if (tempEmailDomains.isEmpty()) {
            return false;
        } else {
            String[] items = email.split("@");

            return tempEmailDomains.contains(items[1]);
        }
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        if (resource == null || !resource.exists()) {
            tempEmailDomains = List.of();
        } else {
            tempEmailDomains = Arrays.asList(resource.getContentAsString(Charset.defaultCharset())
                .split("\n"));
        }
    }
}
