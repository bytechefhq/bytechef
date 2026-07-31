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

package com.bytechef.automation.assetfile.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Operator switch for asset-file public links. When {@code publicLinkEnabled} is false, enabling a link is rejected AND
 * already-enabled links stop resolving — flipping the property is an immediate kill-switch for external sharing, not
 * merely a guard on new links.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.asset-file.sharing")
public record AutomationAssetFileSharingProperties(@DefaultValue("true") boolean publicLinkEnabled) {
}
