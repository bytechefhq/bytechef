
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
            
package com.bytechef.athena.configuration.web.rest.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Generated;

/**
 * A category.
 */

@Schema(name = "Category", description = "A category.")
@JsonTypeName("Category")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-09T13:39:53.714562+02:00[Europe/Zagreb]")
public class CategoryModel {

    private String createdBy;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdDate;

    private Long id;

    private String name;

    private String lastModifiedBy;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime lastModifiedDate;

    private Integer version;

    public CategoryModel() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public CategoryModel(String name) {
        this.name = name;
    }

    public CategoryModel createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    /**
     * The created by.
     *
     * @return createdBy
     */

    @Schema(
        name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public CategoryModel createdDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    /**
     * The created date.
     *
     * @return createdDate
     */
    @Valid
    @Schema(
        name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("createdDate")
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public CategoryModel id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * The id of the category.
     *
     * @return id
     */

    @Schema(name = "id", description = "The id of the category.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategoryModel name(String name) {
        this.name = name;
        return this;
    }

    /**
     * The name of the category.
     *
     * @return name
     */
    @NotNull
    @Schema(name = "name", description = "The name of the category.", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryModel lastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    /**
     * The last modified by.
     *
     * @return lastModifiedBy
     */

    @Schema(
        name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("lastModifiedBy")
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public CategoryModel lastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
        return this;
    }

    /**
     * The last modified date.
     *
     * @return lastModifiedDate
     */
    @Valid
    @Schema(
        name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("lastModifiedDate")
    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public CategoryModel version(Integer version) {
        this.version = version;
        return this;
    }

    /**
     * Get version
     *
     * @return version
     */

    @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("__version")
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CategoryModel category = (CategoryModel) o;
        return Objects.equals(this.createdBy, category.createdBy) &&
            Objects.equals(this.createdDate, category.createdDate) &&
            Objects.equals(this.id, category.id) &&
            Objects.equals(this.name, category.name) &&
            Objects.equals(this.lastModifiedBy, category.lastModifiedBy) &&
            Objects.equals(this.lastModifiedDate, category.lastModifiedDate) &&
            Objects.equals(this.version, category.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdBy, createdDate, id, name, lastModifiedBy, lastModifiedDate, version);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CategoryModel {\n");
        sb.append("    createdBy: ")
            .append(toIndentedString(createdBy))
            .append("\n");
        sb.append("    createdDate: ")
            .append(toIndentedString(createdDate))
            .append("\n");
        sb.append("    id: ")
            .append(toIndentedString(id))
            .append("\n");
        sb.append("    name: ")
            .append(toIndentedString(name))
            .append("\n");
        sb.append("    lastModifiedBy: ")
            .append(toIndentedString(lastModifiedBy))
            .append("\n");
        sb.append("    lastModifiedDate: ")
            .append(toIndentedString(lastModifiedDate))
            .append("\n");
        sb.append("    version: ")
            .append(toIndentedString(version))
            .append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString()
            .replace("\n", "\n    ");
    }
}
