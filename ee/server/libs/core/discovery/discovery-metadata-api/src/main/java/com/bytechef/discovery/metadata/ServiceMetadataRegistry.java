/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.discovery.metadata;

import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ServiceMetadataRegistry {

    void registerMetadata(Map<String, String> metadata);
}
