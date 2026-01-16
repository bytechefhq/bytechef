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

package com.bytechef.platform.component.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.file.storage.domain.FileEntry;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileEntryImplTest {

    @Test
    public void testConstructorWithNulls() {
        FileEntryImpl fileEntryImpl = new FileEntryImpl("filename", null, null, "url");

        assertNull(fileEntryImpl.getExtension());
        assertNull(fileEntryImpl.getMimeType());
        assertEquals("filename", fileEntryImpl.getName());
        assertEquals("url", fileEntryImpl.getUrl());
    }

    @Test
    public void testConstructorWithFileEntry() {
        FileEntry fileEntry = new FileEntry("filename.txt", "url");

        FileEntryImpl fileEntryImpl = new FileEntryImpl(fileEntry);

        assertEquals("txt", fileEntryImpl.getExtension());
        assertEquals("text/plain", fileEntryImpl.getMimeType());
        assertEquals("filename.txt", fileEntryImpl.getName());
        assertEquals("url", fileEntryImpl.getUrl());
    }

    @Test
    public void testGetFileEntry() {
        FileEntryImpl fileEntryImpl = new FileEntryImpl("filename.txt", "txt", "text/plain", "url");

        FileEntry fileEntry = fileEntryImpl.getFileEntry();

        assertEquals("filename.txt", fileEntry.getName());
        assertEquals("url", fileEntry.getUrl());
        // Note: FileEntry(name, url) constructor calculates extension/mimeType from name
        assertEquals("txt", fileEntry.getExtension());
        assertEquals("text/plain", fileEntry.getMimeType());
    }
}
