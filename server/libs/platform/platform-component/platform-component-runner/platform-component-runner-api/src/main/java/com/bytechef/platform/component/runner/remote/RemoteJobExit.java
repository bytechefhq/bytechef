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

package com.bytechef.platform.component.runner.remote;

import org.jspecify.annotations.Nullable;

/**
 * How a remote job ended: {@code finished=false} means the deadline passed first.
 */
public record RemoteJobExit(boolean finished, @Nullable Integer exitCode, @Nullable String reason) {

    public static RemoteJobExit timedOut() {
        return new RemoteJobExit(false, null, null);
    }

    public static RemoteJobExit of(int exitCode) {
        return new RemoteJobExit(true, exitCode, null);
    }

    public static RemoteJobExit of(int exitCode, @Nullable String reason) {
        return new RemoteJobExit(true, exitCode, reason);
    }
}
