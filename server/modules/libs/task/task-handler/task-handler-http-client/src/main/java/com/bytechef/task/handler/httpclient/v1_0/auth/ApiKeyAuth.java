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

package com.bytechef.task.handler.httpclient.v1_0.auth;

import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.*;

import com.bytechef.atlas.Accessor;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeader;
import com.bytechef.task.handler.httpclient.v1_0.param.HTTPQueryParam;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class ApiKeyAuth implements Auth {

    @Override
    public void apply(List<HTTPHeader> headers, List<HTTPQueryParam> queryParameters, Authentication taskAuth) {
        Accessor properties = taskAuth.getProperties();

        if (ApiTokenLocation.valueOf(StringUtils.upperCase(properties.getString(ADD_TO))) == ApiTokenLocation.HEADER) {
            headers.add(new HTTPHeader(properties.getString(KEY), properties.getString(VALUE)));
        } else {
            queryParameters.add(new HTTPQueryParam(properties.getString(KEY), properties.getString(VALUE)));
        }
    }
}
