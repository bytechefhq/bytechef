/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.ai.agent.utils.test.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marko Kriskovic
 * @version ee
 */
public class AiAgentUtilsTestUtils {

    public static String createSkillMd(String name, String description) {
        return "---\nname: " + name + "\ndescription: " + description + "\n---\n\n" + description;
    }

    public static byte[] createZipWithMdFile(String entryName, String content) {
        return createZipWithEntries(Map.of(entryName, content));
    }

    public static byte[] createZipWithEntries(Map<String, String> entries) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String entryValue = entry.getValue();

                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entryValue.getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create test zip", ioException);
        }
    }
}
