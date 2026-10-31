/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class AwsComponentHandlerTest {

    private final ComponentDefinition componentDefinition = new AwsComponentHandler().getDefinition();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/aws_v1.json", componentDefinition);
    }

    @Test
    public void testTheConnectionOffersOneCustomAuthorizationWithEveryAccountField() {
        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations()
            .orElseThrow();

        assertThat(authorizations).hasSize(1);

        Authorization authorization = authorizations.getFirst();

        assertThat(authorization.getType()).isEqualTo(Authorization.AuthorizationType.CUSTOM);

        List<String> propertyNames = authorization.getProperties()
            .orElseThrow()
            .stream()
            .map(Property::getName)
            .toList();

        assertThat(propertyNames).containsExactly(
            "accessKeyId", "secretAccessKey", "region", "roleArn", "externalId");
    }
}
