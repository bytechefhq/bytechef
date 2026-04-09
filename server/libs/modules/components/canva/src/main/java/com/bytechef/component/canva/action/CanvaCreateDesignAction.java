package com.bytechef.component.canva.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION_URL;
import static com.bytechef.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.REFRESH_URL;
import static com.bytechef.component.definition.Authorization.SCOPES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class CanvaCreateDesignAction {
  public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createDesign")
      .title("Create Design")
      .description("Create a Canva design.")
      .metadata(
          Map.of(
              "method", "POST",
  "path", "/designs"
  ,"bodyContentType", BodyContentType.JSON
  ,"mimeType", "application/json"

          )
      )
      .properties(string("type").metadata(
     Map.of(
       "type", PropertyType.BODY
     )
  )
  .label("Type").description(".").required(true),object("design_type").properties(string("type").label("Type").description("The type of design.").options(option("Preset", "preset"),option("Custom", "custom")).required(false),string("name").label("Name").description("The name of the design type.").options(option("Doc", "doc"),option("Email", "email"),option("Presentation", "presentation"),option("Whiteboard", "whiteboard")).required(false),integer("width").minValue(40).maxValue(8000).label("Width").description("The width of the design, in pixels.").required(false),integer("height").minValue(40).maxValue(8000).label("Height").description("The height of the design, in pixels.").required(false)).metadata(
     Map.of(
       "type", PropertyType.BODY
     )
  )
  .label("Design Type").description("The desired design type.").required(false),string("title").minLength(1).maxLength(255).metadata(
     Map.of(
       "type", PropertyType.BODY
     )
  )
  .label("Title").description("The name of the design.").required(false),string("asset_id").metadata(
     Map.of(
       "type", PropertyType.BODY
     )
  )
  .label("Asset Id").description("The ID of an asset to insert into the created design.").required(false))
  .output(outputSchema(object().properties(string("id").description("The design ID.").required(false),object("owner").properties(string("user_id").description("The ID of the user.").required(false),string("team_id").description("The ID of the user's Canva Team.").required(false)).required(false),object("URLs").properties(string("edit_url").description("A temporary editing URL for the design.").required(false),string("view_url").description("A temporary viewing URL for the design.").required(false)).required(false),dateTime("created_at").description("When the design was created in Canva.").required(false),dateTime("updated_at").description("When the design was last updated in Canva.").required(false),string("title").description("The design title.").required(false),object("thumbnail").properties(integer("width").description("The width of the thumbnail image in pixels.").required(false),integer("height").description("The height of the thumbnail image in pixels.").required(false),string("url").description("A URL for retrieving the thumbnail image.").required(false)).description("A thumbnail image representing the object.").required(false),integer("page_count").description("The total number of pages in the design.").required(false)).metadata(
     Map.of(
       "responseType", ResponseType.JSON
     )
  )
  )).help("", "https://docs.bytechef.io/reference/components/canva_v1#create-design");

  private CanvaCreateDesignAction() {
  }
}
