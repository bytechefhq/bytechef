/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import java.net.URL;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomComponentFileStorage {

    void deleteCustomComponentFile(FileEntry componentFile);

    URL getCustomComponentFileURL(FileEntry componentFile);

    FileEntry storeCustomComponentFile(String filename, byte[] bytes);
}
