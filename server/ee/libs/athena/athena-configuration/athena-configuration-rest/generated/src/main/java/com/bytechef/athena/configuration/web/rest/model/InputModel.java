
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
 * InputModel
 */

@JsonTypeName("Input")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-10-09T13:39:53.714562+02:00[Europe/Zagreb]")
public class InputModel {

    private String label;

    private String name;

    private Boolean required = false;

    private String type;

    public InputModel() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public InputModel(String name) {
        this.name = name;
    }

    public InputModel label(String label) {
        this.label = label;
        return this;
    }

    /**
     * The descriptive name of an input
     *
     * @return label
     */

    @Schema(
        name = "label", description = "The descriptive name of an input",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public InputModel name(String name) {
        this.name = name;
        return this;
    }

    /**
     * The name of an input
     *
     * @return name
     */
    @NotNull
    @Schema(name = "name", description = "The name of an input", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InputModel required(Boolean required) {
        this.required = required;
        return this;
    }

    /**
     * If an input is required, or not
     *
     * @return required
     */

    @Schema(
        name = "required", description = "If an input is required, or not",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("required")
    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public InputModel type(String type) {
        this.type = type;
        return this;
    }

    /**
     * The type of an input, for example \\\"string\\\"
     *
     * @return type
     */

    @Schema(
        name = "type", description = "The type of an input, for example \\\"string\\\"",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InputModel input = (InputModel) o;
        return Objects.equals(this.label, input.label) &&
            Objects.equals(this.name, input.name) &&
            Objects.equals(this.required, input.required) &&
            Objects.equals(this.type, input.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, name, required, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class InputModel {\n");
        sb.append("    label: ")
            .append(toIndentedString(label))
            .append("\n");
        sb.append("    name: ")
            .append(toIndentedString(name))
            .append("\n");
        sb.append("    required: ")
            .append(toIndentedString(required))
            .append("\n");
        sb.append("    type: ")
            .append(toIndentedString(type))
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
