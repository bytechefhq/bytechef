package com.bytechef.component.canva;

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

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.canva.action.CanvaCreateDesignAction;
import com.bytechef.component.canva.action.CanvaExportDesignAction;
import com.bytechef.component.canva.action.CanvaUploadAssetAction;
import com.bytechef.component.canva.connection.CanvaConnection;
import com.bytechef.component.definition.ComponentDefinition;
import java.lang.Override;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractCanvaComponentHandler implements OpenApiComponentHandler {
  private final ComponentDefinition componentDefinition = modifyComponent(
      component("canva")
          .title("Canva")
          .description("Canva is a web and mobile application designed to help users create, design, and collaborate on visual content such as graphics, presentations, and marketing materials easily and efficiently.")
          .version(1)
      )
      .actions(modifyActions(CanvaCreateDesignAction.ACTION_DEFINITION,CanvaExportDesignAction.ACTION_DEFINITION,CanvaUploadAssetAction.ACTION_DEFINITION))
  .connection(modifyConnection(CanvaConnection.CONNECTION_DEFINITION)).clusterElements(modifyClusterElements(tool(CanvaCreateDesignAction.ACTION_DEFINITION),tool(CanvaExportDesignAction.ACTION_DEFINITION),tool(CanvaUploadAssetAction.ACTION_DEFINITION))).triggers(getTriggers());

  @Override
  public ComponentDefinition getDefinition() {
    return componentDefinition;
  }
}
