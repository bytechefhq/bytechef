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

package com.bytechef.automation.assetfile.exception;

/**
 * Thrown when an asset file is not found in a given workspace, either because it does not exist or because it belongs
 * to a different workspace. The two cases are intentionally indistinguishable so that workspace members cannot
 * enumerate asset file ids across the rest of the system.
 *
 * <p>
 * REST callers should map this to HTTP 404 via an explicit {@code @ExceptionHandler}.
 *
 * @author Ivica Cardic
 */
public class AssetFileNotFoundException extends RuntimeException {

    public AssetFileNotFoundException(String message) {
        super(message);
    }

    public AssetFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
