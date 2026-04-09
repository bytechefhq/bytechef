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
public class CanvaUploadAssetAction {
  public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("uploadAsset")
      .title("Get asset upload job")
      .description("Get the status and results of an upload asset job.")
      .metadata(
          Map.of(
              "method", "GET",
  "path", "/asset-uploads/{jobId}"

          )
      )
      .properties(string("jobId").label("job ID").description("The asset upload job ID.").required(true).metadata(
     Map.of(
       "type", PropertyType.PATH
     )
  )
  )
  .output(outputSchema(object().properties(string("id").description("The ID of the asset upload job.").required(false),string("status").description("Status of the asset upload job.").required(false),object("asset").properties(string("type").description("Type of an asset.").required(false),string("id").description("The ID of the asset.").required(false),string("name").description("The name of the asset.").required(false),array("tags").items(string().description("The user-facing tags attached to the asset.")).description("The user-facing tags attached to the asset.").required(false),dateTime("created_at").description("When the asset was added to Canva.").required(false),dateTime("updated_at").description("When the asset was last updated in Canva.").required(false),object("owner").properties(string("user_id").description("The ID of the user.").required(false),string("team_id").description("The ID of the user's Canva Team.").required(false)).description("Metadata for the user.").required(false),object("thumbnail").properties(integer("width").description("The width of the thumbnail image in pixels.").required(false),integer("height").description("The height of the thumbnail image in pixels.").required(false),string("url").description("A URL for retrieving the thumbnail image.").required(false)).description("A thumbnail image representing the object.").required(false),object("metadata").properties(string("type").description("Type of an asset.").required(false),integer("width").description("The width of the image in pixels.").required(false),integer("height").description("The height of the image in pixels.").required(false),array("smart_tags").items(string().description("AI-generated tags for the image.")).description("AI-generated tags for the image.").required(false)).description("Type-specific metadata for the asset.").required(false)).description("The asset object, which contains metadata about the asset.").required(false)).metadata(
     Map.of(
       "responseType", ResponseType.JSON
     )
  )
  )).help("", "https://docs.bytechef.io/reference/components/canva_v1#upload-asset");

  private CanvaUploadAssetAction() {
  }
}
