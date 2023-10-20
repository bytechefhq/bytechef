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

package com.integri.atlas.task.handler.httpclient.auth;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.*;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.httpclient.header.HttpHeader;
import com.integri.atlas.task.handler.httpclient.params.HttpQueryParam;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class ApiKeyHttpAuth implements HttpAuth {

    @Override
    public void apply(List<HttpHeader> headers, List<HttpQueryParam> queryParameters, TaskAuth taskAuth) {
        Accessor properties = taskAuth.getProperties();

        if (ApiTokenLocation.valueOf(StringUtils.upperCase(properties.getString(ADD_TO))) == ApiTokenLocation.HEADER) {
            headers.add(new HttpHeader(properties.getString(KEY), properties.getString(VALUE)));
        } else {
            queryParameters.add(new HttpQueryParam(properties.getString(KEY), properties.getString(VALUE)));
        }
    }
}
