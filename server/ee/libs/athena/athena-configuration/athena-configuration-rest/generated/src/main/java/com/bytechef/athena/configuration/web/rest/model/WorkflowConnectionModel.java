
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
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.annotation.Generated;

/**
 * WorkflowConnectionModel
 */

@JsonTypeName("WorkflowConnection")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-09T13:39:53.714562+02:00[Europe/Zagreb]")
public class WorkflowConnectionModel {

    private String componentName;

    private Integer componentVersion;

    private String key;

    private String operationName;

    public WorkflowConnectionModel() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public WorkflowConnectionModel(String componentName, Integer componentVersion, String key, String operationName) {
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.key = key;
        this.operationName = operationName;
    }

    public WorkflowConnectionModel componentName(String componentName) {
        this.componentName = componentName;
        return this;
    }

    /**
     * Get componentName
     *
     * @return componentName
     */
    @NotNull
    @Schema(name = "componentName", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("componentName")
    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public WorkflowConnectionModel componentVersion(Integer componentVersion) {
        this.componentVersion = componentVersion;
        return this;
    }

    /**
     * Get componentVersion
     *
     * @return componentVersion
     */
    @NotNull
    @Schema(name = "componentVersion", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("componentVersion")
    public Integer getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(Integer componentVersion) {
        this.componentVersion = componentVersion;
    }

    public WorkflowConnectionModel key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Get key
     *
     * @return key
     */
    @NotNull
    @Schema(name = "key", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public WorkflowConnectionModel operationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    /**
     * Get operationName
     *
     * @return operationName
     */
    @NotNull
    @Schema(name = "operationName", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("operationName")
    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkflowConnectionModel workflowConnection = (WorkflowConnectionModel) o;
        return Objects.equals(this.componentName, workflowConnection.componentName) &&
            Objects.equals(this.componentVersion, workflowConnection.componentVersion) &&
            Objects.equals(this.key, workflowConnection.key) &&
            Objects.equals(this.operationName, workflowConnection.operationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, componentVersion, key, operationName);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class WorkflowConnectionModel {\n");
        sb.append("    componentName: ")
            .append(toIndentedString(componentName))
            .append("\n");
        sb.append("    componentVersion: ")
            .append(toIndentedString(componentVersion))
            .append("\n");
        sb.append("    key: ")
            .append(toIndentedString(key))
            .append("\n");
        sb.append("    operationName: ")
            .append(toIndentedString(operationName))
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
