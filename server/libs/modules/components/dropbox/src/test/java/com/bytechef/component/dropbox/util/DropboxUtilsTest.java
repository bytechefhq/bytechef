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

package com.bytechef.component.dropbox.util;

import static com.bytechef.component.dropbox.util.DropboxUtils.getFullPath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class DropboxUtilsTest {

    @Test
    void testGetFullPath() {
        String path1 = "folder1/";
        String filename1 = "file1.txt";

        assertEquals("folder1/file1.txt", getFullPath(path1, filename1));

        String path2 = "folder2";
        String filename2 = "file2.txt";

        assertEquals("folder2/file2.txt", getFullPath(path2, filename2));
    }
}
