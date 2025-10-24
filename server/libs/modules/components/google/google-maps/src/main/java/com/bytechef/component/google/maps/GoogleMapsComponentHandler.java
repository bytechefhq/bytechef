/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.google.maps;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.maps.action.GoogleMapsGetAddressAction;
import com.bytechef.component.google.maps.action.GoogleMapsGetGeolocationAction;
import com.bytechef.component.google.maps.action.GoogleMapsGetRouteAction;
import com.bytechef.component.google.maps.action.GoogleMapsNearbySearchAction;
import com.bytechef.component.google.maps.connection.GoogleMapsConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class GoogleMapsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleMaps")
        .title("Google Maps")
        .description(
            "Google Maps is a widely used mapping service by Google, offering free and feature-rich navigation, " +
                "location discovery, and real-time traffic updates accessible through web browsers and mobile apps.")
        .icon("path:assets/google-maps.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(GoogleMapsConnection.CONNECTION_DEFINITION)
        .clusterElements(
            tool(GoogleMapsGetAddressAction.ACTION_DEFINITION),
            tool(GoogleMapsGetGeolocationAction.ACTION_DEFINITION),
            tool(GoogleMapsGetRouteAction.ACTION_DEFINITION),
            tool(GoogleMapsNearbySearchAction.ACTION_DEFINITION))
        .actions(
            GoogleMapsGetAddressAction.ACTION_DEFINITION,
            GoogleMapsGetGeolocationAction.ACTION_DEFINITION,
            GoogleMapsGetRouteAction.ACTION_DEFINITION,
            GoogleMapsNearbySearchAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
