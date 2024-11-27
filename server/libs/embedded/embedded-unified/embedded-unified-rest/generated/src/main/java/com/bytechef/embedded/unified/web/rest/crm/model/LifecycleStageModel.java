package com.bytechef.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets lifecycle_stage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:57.351813+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public enum LifecycleStageModel {
  
  SUBSCRIBER("SUBSCRIBER"),
  
  LEAD("LEAD"),
  
  MARKETING_QUALIFIED_LEAD("MARKETING_QUALIFIED_LEAD"),
  
  SALES_QUALIFIED_LEAD("SALES_QUALIFIED_LEAD"),
  
  OPPORTUNITY("OPPORTUNITY"),
  
  CUSTOMER("CUSTOMER"),
  
  EVANGELIST("EVANGELIST"),
  
  OTHER("OTHER");

  private String value;

  LifecycleStageModel(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static LifecycleStageModel fromValue(String value) {
    for (LifecycleStageModel b : LifecycleStageModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}

