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

package com.integri.atlas.task.handler.http.client.header;

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.BodyContentType;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_BODY_CONTENT_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_FILE_ENTRY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_HEADER_PARAMETERS;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_KEY;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_MIME_TYPE;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_RESPONSE_FORMAT;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_VALUE;
import static com.integri.atlas.task.handler.http.client.header.HttpHeader.BOUNDARY_TMPL;

import com.integri.atlas.engine.task.execution.TaskExecution;
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
public class HttpHeadersFactory {

    @SuppressWarnings("unchecked")
    public List<HttpHeader> getHttpHeaders(TaskExecution taskExecution) {
        Set<HttpHeader> httpHeaders = new HashSet<>();

        if (taskExecution.containsKey(PROPERTY_HEADER_PARAMETERS)) {
            httpHeaders.addAll(fromHeaderParameters(taskExecution.get(PROPERTY_HEADER_PARAMETERS, List.class)));
        }

        if (taskExecution.containsKey(PROPERTY_RESPONSE_FORMAT)) {
            httpHeaders.add(new HttpHeader("Accept", MimeTypeUtils.ALL_VALUE));
        }

        if (taskExecution.containsKey(PROPERTY_BODY_CONTENT_TYPE)) {
            httpHeaders.add(new HttpHeader("Content-Type", getContentTypeValue(taskExecution)));
        }

        httpHeaders.addAll(getUserDefinedHttpHeaders(taskExecution));

        return new ArrayList<>(httpHeaders);
    }

    private String getContentTypeValue(TaskExecution taskExecution) {
        BodyContentType bodyContentType = BodyContentType.valueOf(taskExecution.get(PROPERTY_BODY_CONTENT_TYPE));

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
            return taskExecution.getString(PROPERTY_MIME_TYPE);
        }

        if (bodyContentType == BodyContentType.BINARY) {
            Tika tika = new Tika();

            return tika.detect(taskExecution.getString(PROPERTY_FILE_ENTRY));
        }

        throw new IllegalArgumentException("Invalid body content type " + bodyContentType);
    }

    private Set<HttpHeader> getUserDefinedHttpHeaders(TaskExecution taskExecution) {
        Set<HttpHeader> httpHeaders = new HashSet<>();

        if (taskExecution.containsKey(PROPERTY_HEADER_PARAMETERS)) {
            httpHeaders.addAll(fromHeaderParameters(taskExecution.get(PROPERTY_HEADER_PARAMETERS)));
        }

        return httpHeaders;
    }

    private List<HttpHeader> fromHeaderParameters(List<Map<String, String>> headerParameters) {
        List<HttpHeader> httpHeaders = new ArrayList<>();

        for (Map<String, String> parameter : headerParameters) {
            httpHeaders.add(new HttpHeader(parameter.get(PROPERTY_KEY), parameter.get(PROPERTY_VALUE)));
        }

        return httpHeaders;
    }
}
