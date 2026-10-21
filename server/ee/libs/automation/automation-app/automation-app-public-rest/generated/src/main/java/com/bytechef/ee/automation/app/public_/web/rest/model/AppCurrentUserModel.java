package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The signed-in user of the request.
 */

@Schema(name = "AppCurrentUser", description = "The signed-in user of the request.")
@JsonTypeName("AppCurrentUser")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T16:22:03.857597+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppCurrentUserModel {

  private Long id;

  private String login;

  private JsonNullable<String> firstName = JsonNullable.<String>undefined();

  private JsonNullable<String> lastName = JsonNullable.<String>undefined();

  private JsonNullable<String> email = JsonNullable.<String>undefined();

  private JsonNullable<String> workspaceRole = JsonNullable.<String>undefined();

  private List<String> scopes = new ArrayList<>();

  public AppCurrentUserModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppCurrentUserModel(Long id, String login, List<String> scopes) {
    this.id = id;
    this.login = login;
    this.scopes = scopes;
  }

  public AppCurrentUserModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(Long id) {
    this.id = id;
  }

  public AppCurrentUserModel login(String login) {
    this.login = login;
    return this;
  }

  /**
   * Get login
   * @return login
   */
  @NotNull 
  @Schema(name = "login", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("login")
  public String getLogin() {
    return login;
  }

  @JsonProperty("login")
  public void setLogin(String login) {
    this.login = login;
  }

  public AppCurrentUserModel firstName(String firstName) {
    this.firstName = JsonNullable.of(firstName);
    return this;
  }

  /**
   * Get firstName
   * @return firstName
   */
  
  @Schema(name = "firstName", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("firstName")
  public JsonNullable<String> getFirstName() {
    return firstName;
  }

  public void setFirstName(JsonNullable<String> firstName) {
    this.firstName = firstName;
  }

  public AppCurrentUserModel lastName(String lastName) {
    this.lastName = JsonNullable.of(lastName);
    return this;
  }

  /**
   * Get lastName
   * @return lastName
   */
  
  @Schema(name = "lastName", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("lastName")
  public JsonNullable<String> getLastName() {
    return lastName;
  }

  public void setLastName(JsonNullable<String> lastName) {
    this.lastName = lastName;
  }

  public AppCurrentUserModel email(String email) {
    this.email = JsonNullable.of(email);
    return this;
  }

  /**
   * Get email
   * @return email
   */
  
  @Schema(name = "email", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("email")
  public JsonNullable<String> getEmail() {
    return email;
  }

  public void setEmail(JsonNullable<String> email) {
    this.email = email;
  }

  public AppCurrentUserModel workspaceRole(String workspaceRole) {
    this.workspaceRole = JsonNullable.of(workspaceRole);
    return this;
  }

  /**
   * The user's built-in workspace role in the instance's environment (ADMIN, EDITOR or VIEWER); null when the user holds a custom role there or none.
   * @return workspaceRole
   */
  
  @Schema(name = "workspaceRole", description = "The user's built-in workspace role in the instance's environment (ADMIN, EDITOR or VIEWER); null when the user holds a custom role there or none.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("workspaceRole")
  public JsonNullable<String> getWorkspaceRole() {
    return workspaceRole;
  }

  public void setWorkspaceRole(JsonNullable<String> workspaceRole) {
    this.workspaceRole = workspaceRole;
  }

  public AppCurrentUserModel scopes(List<String> scopes) {
    this.scopes = scopes;
    return this;
  }

  public AppCurrentUserModel addScopesItem(String scopesItem) {
    if (this.scopes == null) {
      this.scopes = new ArrayList<>();
    }
    this.scopes.add(scopesItem);
    return this;
  }

  /**
   * The app scopes the user holds on the instance, out of APP_VIEW, APP_USE, APP_DEPLOY and APP_MANAGE.
   * @return scopes
   */
  @NotNull 
  @Schema(name = "scopes", description = "The app scopes the user holds on the instance, out of APP_VIEW, APP_USE, APP_DEPLOY and APP_MANAGE.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("scopes")
  public List<String> getScopes() {
    return scopes;
  }

  @JsonProperty("scopes")
  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppCurrentUserModel appCurrentUser = (AppCurrentUserModel) o;
    return Objects.equals(this.id, appCurrentUser.id) &&
        Objects.equals(this.login, appCurrentUser.login) &&
        equalsNullable(this.firstName, appCurrentUser.firstName) &&
        equalsNullable(this.lastName, appCurrentUser.lastName) &&
        equalsNullable(this.email, appCurrentUser.email) &&
        equalsNullable(this.workspaceRole, appCurrentUser.workspaceRole) &&
        Objects.equals(this.scopes, appCurrentUser.scopes);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, login, hashCodeNullable(firstName), hashCodeNullable(lastName), hashCodeNullable(email), hashCodeNullable(workspaceRole), scopes);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppCurrentUserModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    login: ").append(toIndentedString(login)).append("\n");
    sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
    sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    workspaceRole: ").append(toIndentedString(workspaceRole)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

