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

package com.bytechef.task.handler.httpclient.v1_0.header;

import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BODY_CONTENT_TYPE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BodyContentType;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.HEADER_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.MIME_TYPE;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.RESPONSE_FORMAT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.VALUE;
import static com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeader.BOUNDARY_TMPL;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Matija Petanjek
 */
@Component
public class HTTPHeaderFactory {

    @SuppressWarnings("unchecked")
    public List<HTTPHeader> getHTTPHeaders(TaskExecution taskExecution) {
        Set<HTTPHeader> httpHeaders = new HashSet<>();

        if (taskExecution.containsKey(HEADER_PARAMETERS)) {
            httpHeaders.addAll(fromHeaderParameters(taskExecution.get(HEADER_PARAMETERS, List.class)));
        }

        if (taskExecution.containsKey(RESPONSE_FORMAT)) {
            httpHeaders.add(new HTTPHeader("Accept", MimeTypeUtils.ALL_VALUE));
        }

        if (taskExecution.containsKey(BODY_CONTENT_TYPE)) {
            httpHeaders.add(new HTTPHeader("Content-Type", getContentTypeValue(taskExecution)));
        }

        httpHeaders.addAll(getUserDefinedHTTPHeaders(taskExecution));

        return new ArrayList<>(httpHeaders);
    }

    private String getContentTypeValue(TaskExecution taskExecution) {
        BodyContentType bodyContentType = BodyContentType.valueOf(taskExecution.get(BODY_CONTENT_TYPE));

        if (bodyContentType == BodyContentType.JSON) {
            return MimeTypeUtils.APPLICATION_JSON_VALUE;
        }

        if (bodyContentType == BodyContentType.FORM_URLENCODED) {
            return "application/x-www-form-urlencoded";
        }

        if (bodyContentType == BodyContentType.FORM_DATA) {
            return "multipart/form-data; boundary=" + BOUNDARY_TMPL;
        }

        if (bodyContentType == BodyContentType.RAW) {
            return taskExecution.getString(MIME_TYPE);
        }

        if (bodyContentType == BodyContentType.BINARY) {
            Tika tika = new Tika();

            return tika.detect(taskExecution.getString(FILE_ENTRY));
        }

        throw new IllegalArgumentException("Invalid body content type " + bodyContentType);
    }

    private Set<HTTPHeader> getUserDefinedHTTPHeaders(TaskExecution taskExecution) {
        Set<HTTPHeader> httpHeaders = new HashSet<>();

        if (taskExecution.containsKey(HEADER_PARAMETERS)) {
            httpHeaders.addAll(fromHeaderParameters(taskExecution.get(HEADER_PARAMETERS)));
        }

        return httpHeaders;
    }

    private List<HTTPHeader> fromHeaderParameters(List<Map<String, String>> headerParameters) {
        List<HTTPHeader> httpHeaders = new ArrayList<>();

        for (Map<String, String> parameter : headerParameters) {
            httpHeaders.add(new HTTPHeader(parameter.get(KEY), parameter.get(VALUE)));
        }

        return httpHeaders;
    }
}
