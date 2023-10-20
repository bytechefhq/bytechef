
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.athena.configuration.dto;

import com.bytechef.athena.configuration.domain.Integration;
import com.bytechef.category.domain.Category;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDTO(
    Category category, String createdBy, LocalDateTime createdDate, String description, Long id, int integrationVersion,
    String name, String lastModifiedBy, LocalDateTime lastModifiedDate, LocalDateTime publishedDate,
    Integration.Status status, List<Tag> tags, int version, List<String> workflowIds) {

    public IntegrationDTO(Integration integration, Category category, List<Tag> tags) {
        this(
            category, integration.getCreatedBy(), integration.getCreatedDate(),
            integration.getDescription(), integration.getId(), integration.getIntegrationVersion(),
            integration.getName(), integration.getLastModifiedBy(), integration.getLastModifiedDate(),
            integration.getPublishedDate(), integration.getStatus(), tags, integration.getVersion(),
            integration.getWorkflowIds());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integration toIntegration() {
        Integration integration = new Integration();

        integration.setCategory(category);
        integration.setDescription(description);
        integration.setId(id);
        integration.setName(name);
        integration.setIntegrationVersion(integrationVersion);
        integration.setPublishedDate(publishedDate);
        integration.setStatus(status);
        integration.setTags(tags);
        integration.setVersion(version);
        integration.setWorkflowIds(workflowIds);

        return integration;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Category category;
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private Long id;
        private String name;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private int integrationVersion;
        private LocalDateTime publishedDate;
        private Integration.Status status;
        private List<Tag> tags;
        private int version;
        private List<String> workflowIds;

        private Builder() {
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public Builder integrationVersion(int integrationVersion) {
            this.integrationVersion = integrationVersion;
            return this;
        }

        public Builder publishedDate(LocalDateTime publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder status(Integration.Status status) {
            this.status = status;
            return this;
        }

        public Builder tags(List<Tag> tags) {
            this.tags = tags;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder workflowIds(List<String> workflowIds) {
            this.workflowIds = workflowIds;
            return this;
        }

        public IntegrationDTO build() {
            return new IntegrationDTO(
                category, createdBy, createdDate, description, id, integrationVersion, name, lastModifiedBy,
                lastModifiedDate,
                publishedDate, status, tags, version, workflowIds);
        }
    }
}
