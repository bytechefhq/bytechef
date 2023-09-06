
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.file.storage.converter;

import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class FileEntryToStringConverter implements Converter<FileEntry, String> {

    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public FileEntryToStringConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convert(FileEntry fileEntry) {
        try {
            return objectMapper.writeValueAsString(fileEntry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
