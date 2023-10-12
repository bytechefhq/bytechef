/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.commons.util;

import java.io.IOException;
import java.io.Reader;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class FileCopyUtils {

    public static String copyToString(@Nullable Reader in) throws IOException {
        return org.springframework.util.FileCopyUtils.copyToString(in);
    }
}
