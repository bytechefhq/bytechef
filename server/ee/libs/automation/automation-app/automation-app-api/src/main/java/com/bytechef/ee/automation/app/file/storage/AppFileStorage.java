/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.file.storage;

import com.bytechef.file.storage.domain.FileEntry;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppFileStorage {

    void deleteIcon(FileEntry fileEntry);

    byte[] readIcon(FileEntry fileEntry);

    FileEntry storeIcon(String filename, byte[] content);
}
