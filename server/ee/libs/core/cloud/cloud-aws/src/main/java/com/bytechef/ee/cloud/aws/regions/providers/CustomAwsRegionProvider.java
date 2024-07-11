/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cloud.aws.regions.providers;

import com.bytechef.config.ApplicationProperties.Cloud.Aws;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CustomAwsRegionProvider implements AwsRegionProvider {

    private final Aws aws;

    @SuppressFBWarnings("EI")
    public CustomAwsRegionProvider(Aws aws) {
        this.aws = aws;
    }

    @Override
    public Region getRegion() {
        return Region.of(aws.getRegion());
    }
}
