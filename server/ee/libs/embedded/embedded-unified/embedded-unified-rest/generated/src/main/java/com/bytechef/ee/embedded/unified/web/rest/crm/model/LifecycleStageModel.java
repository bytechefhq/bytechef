package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets lifecycle_stage
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-05-11T23:38:05.784253+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
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

