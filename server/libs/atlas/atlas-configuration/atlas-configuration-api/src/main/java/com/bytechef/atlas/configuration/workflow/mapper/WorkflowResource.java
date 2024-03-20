/*
 * Copyright 2016-2020 the original author or authors.
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
 *
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.atlas.configuration.workflow.mapper;

import com.bytechef.atlas.configuration.domain.Workflow.Format;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.io.Resource;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 */
public class WorkflowResource implements Resource {

    private final String id;
    private final long lastModified;
    private final Map<String, Object> metadata;
    private final transient Resource resource;
    private final Format workflowFormat;

    public WorkflowResource(
        String id, Map<String, Object> metadata, Resource resource, Format workflowFormat) {

        this(id, 0, metadata, resource, workflowFormat);
    }

    public WorkflowResource(
        String id, long lastModified, Map<String, Object> metadata, Resource resource, Format workflowFormat) {

        this.id = id;
        this.lastModified = lastModified;
        this.metadata = new HashMap<>(metadata);
        this.resource = resource;
        this.workflowFormat = workflowFormat;
    }

    public String getId() {
        return id;
    }

    public Format getWorkflowFormat() {
        return workflowFormat;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return resource.getInputStream();
    }

    @Override
    public boolean exists() {
        return resource.exists();
    }

    @Override
    public boolean isReadable() {
        return resource.isReadable();
    }

    @Override
    public boolean isOpen() {
        return resource.isOpen();
    }

    @Override
    public URL getURL() throws IOException {
        return resource.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return resource.getURI();
    }

    @Override
    public File getFile() throws IOException {
        return resource.getFile();
    }

    @Override
    public long contentLength() throws IOException {
        return resource.contentLength();
    }

    @Override
    public long lastModified() throws IOException {
        if (lastModified == 0) {
            return resource.lastModified();
        }

        return lastModified;
    }

    @Override
    public Resource createRelative(String aRelativePath) throws IOException {
        return resource.createRelative(aRelativePath);
    }

    @Override
    public String getFilename() {
        return resource.getFilename();
    }

    @Override
    public String getDescription() {
        return resource.getDescription();
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
