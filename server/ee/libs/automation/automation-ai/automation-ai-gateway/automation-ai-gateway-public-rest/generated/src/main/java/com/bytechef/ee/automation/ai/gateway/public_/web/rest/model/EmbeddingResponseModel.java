package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingDataModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.EmbeddingUsageModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An embedding response.
 */

@Schema(name = "EmbeddingResponse", description = "An embedding response.")
@JsonTypeName("EmbeddingResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class EmbeddingResponseModel {

  private @Nullable String _object;

  @Valid
  private List<@Valid EmbeddingDataModel> data = new ArrayList<>();

  private @Nullable String model;

  private @Nullable EmbeddingUsageModel usage;

  public EmbeddingResponseModel _object(@Nullable String _object) {
    this._object = _object;
    return this;
  }

  /**
   * Get _object
   * @return _object
   */
  
  @Schema(name = "object", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("object")
  public @Nullable String getObject() {
    return _object;
  }

  public void setObject(@Nullable String _object) {
    this._object = _object;
  }

  public EmbeddingResponseModel data(List<@Valid EmbeddingDataModel> data) {
    this.data = data;
    return this;
  }

  public EmbeddingResponseModel addDataItem(EmbeddingDataModel dataItem) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }
    this.data.add(dataItem);
    return this;
  }

  /**
   * Get data
   * @return data
   */
  @Valid 
  @Schema(name = "data", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("data")
  public List<@Valid EmbeddingDataModel> getData() {
    return data;
  }

  public void setData(List<@Valid EmbeddingDataModel> data) {
    this.data = data;
  }

  public EmbeddingResponseModel model(@Nullable String model) {
    this.model = model;
    return this;
  }

  /**
   * Get model
   * @return model
   */
  
  @Schema(name = "model", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("model")
  public @Nullable String getModel() {
    return model;
  }

  public void setModel(@Nullable String model) {
    this.model = model;
  }

  public EmbeddingResponseModel usage(@Nullable EmbeddingUsageModel usage) {
    this.usage = usage;
    return this;
  }

  /**
   * Get usage
   * @return usage
   */
  @Valid 
  @Schema(name = "usage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("usage")
  public @Nullable EmbeddingUsageModel getUsage() {
    return usage;
  }

  public void setUsage(@Nullable EmbeddingUsageModel usage) {
    this.usage = usage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmbeddingResponseModel embeddingResponse = (EmbeddingResponseModel) o;
    return Objects.equals(this._object, embeddingResponse._object) &&
        Objects.equals(this.data, embeddingResponse.data) &&
        Objects.equals(this.model, embeddingResponse.model) &&
        Objects.equals(this.usage, embeddingResponse.usage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_object, data, model, usage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmbeddingResponseModel {\n");
    sb.append("    _object: ").append(toIndentedString(_object)).append("\n");
    sb.append("    data: ").append(toIndentedString(data)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    usage: ").append(toIndentedString(usage)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

