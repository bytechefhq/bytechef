package com.bytechef.platform.configuration.web.rest.model;

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
 * A category of component.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-10T12:18:15.141178+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public enum ComponentCategoryModel {
  
  ACCOUNTING("accounting"),
  
  ADVERTISING("advertising"),
  
  ANALYTICS("analytics"),
  
  ARTIFICIAL_INTELLIGENCE("artificial-intelligence"),
  
  ATS("ats"),
  
  CALENDARS_AND_SCHEDULING("calendars-and-scheduling"),
  
  COMMUNICATION("communication"),
  
  CRM("crm"),
  
  CUSTOMER_SUPPORT("customer-support"),
  
  DEVELOPER_TOOLS("developer-tools"),
  
  E_COMMERCE("e-commerce"),
  
  FILE_STORAGE("file-storage"),
  
  HELPERS("helpers"),
  
  HRIS("hris"),
  
  MARKETING_AUTOMATION("marketing-automation"),
  
  PAYMENT_PROCESSING("payment-processing"),
  
  PRODUCTIVITY_AND_COLLABORATION("productivity-and-collaboration"),
  
  PROJECT_MANAGEMENT("project-management"),
  
  SOCIAL_MEDIA("social-media"),
  
  SURVEYS_AND_FEEDBACK("surveys-and-feedback");

  private String value;

  ComponentCategoryModel(String value) {
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
  public static ComponentCategoryModel fromValue(String value) {
    for (ComponentCategoryModel b : ComponentCategoryModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

