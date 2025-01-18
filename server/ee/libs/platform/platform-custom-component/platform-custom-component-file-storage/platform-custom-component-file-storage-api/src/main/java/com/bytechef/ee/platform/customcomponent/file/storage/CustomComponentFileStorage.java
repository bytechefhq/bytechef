/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.file.storage;

import com.bytechef.file.storage.domain.FileEntry;
import java.net.URL;
import org.springframework.lang.NonNull;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomComponentFileStorage {

    void deleteCustomComponentFile(@NonNull FileEntry componentFile);

    URL getCustomComponentFileURL(@NonNull FileEntry componentFile);

    FileEntry storeCustomComponentFile(@NonNull String filename, @NonNull byte[] bytes);
}
