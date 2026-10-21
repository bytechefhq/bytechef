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

package com.bytechef.cli.command.automation;

/**
 * The contents of {@code .bytechef/app.json}: which app a directory belongs to and the ByteChef target it was made on.
 * It never holds a secret, so it is safe to commit.
 *
 * @author Ivica Cardic
 */
public record AppLink(long id, String uuid, long workspaceId, String environment, String host) {
}
