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
 * A category of unified API.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:59.239958+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public enum UnifiedApiCategoryModel {
  
  ACCOUNTING("ACCOUNTING"),
  
  ATS("ATS"),
  
  CRM("CRM"),
  
  E_COMMERCE("E_COMMERCE"),
  
  HRIS("HRIS"),
  
  FILE_STORAGE("FILE_STORAGE"),
  
  MARKETING_AUTOMATION("MARKETING_AUTOMATION"),
  
  TICKETING("TICKETING");

  private String value;

  UnifiedApiCategoryModel(String value) {
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
  public static UnifiedApiCategoryModel fromValue(String value) {
    for (UnifiedApiCategoryModel b : UnifiedApiCategoryModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

