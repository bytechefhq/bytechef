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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.ENCODING;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILENAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public class FileStorageWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("write")
        .title("Write to File")
        .description("Writes the data to the file.")
        .properties(
            string(CONTENT)
                .label("Content")
                .description("String to write to the file.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Filename to set for data. By default, \"file.txt\" will be used.")
                .defaultValue("file.txt"),
            string(ENCODING)
                .label("Encoding")
                .description(
                    "Character encoding the content is written in. A character the encoding cannot represent " +
                        "fails the action.")
                .options(
                    option("UTF-8", "UTF-8"),
                    option("UTF-16", "UTF-16"),
                    option("US-ASCII", "US-ASCII"),
                    option("ISO-8859-1 (Latin-1)", "ISO-8859-1"),
                    option("ISO-8859-2 (Latin-2)", "ISO-8859-2"),
                    option("ISO-8859-15 (Latin-9)", "ISO-8859-15"),
                    option("Windows-1250", "windows-1250"),
                    option("Windows-1251", "windows-1251"),
                    option("Windows-1252", "windows-1252"))
                .defaultValue("UTF-8"))
        .output(outputSchema(fileEntry().description("File entry with the written data.")))
        .help("", "https://docs.bytechef.io/reference/components/file-storage_v1#write-to-file")
        .perform(FileStorageWriteAction::perform);

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object content = inputParameters.getRequired(CONTENT);
        String fileName = inputParameters.getString(FILENAME, "file.txt");
        Charset charset = Charset.forName(inputParameters.getString(ENCODING, "UTF-8"));

        String data = content instanceof String string ? string : content.toString();

        if (charset.equals(StandardCharsets.UTF_8)) {
            return context.file(file -> file.storeContent(fileName, data));
        }

        byte[] bytes = encode(data, charset);

        return context.file(file -> file.storeContent(fileName, new ByteArrayInputStream(bytes)));
    }

    private static byte[] encode(String data, Charset charset) {
        CharsetEncoder charsetEncoder = charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            ByteBuffer byteBuffer = charsetEncoder.encode(CharBuffer.wrap(data));

            byte[] bytes = new byte[byteBuffer.remaining()];

            byteBuffer.get(bytes);

            return bytes;
        } catch (CharacterCodingException characterCodingException) {
            throw new IllegalArgumentException(
                "The content has a character that cannot be encoded in " + charset.name(), characterCodingException);
        }
    }
}
