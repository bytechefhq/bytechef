/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.mapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum FieldMappingDirection {

    TO_INTEGRATION(
        "mapToIntegration", "Map to Integration Object",
        "Renames an application object's keys to the connected user's integration fields, ready to write into " +
            "their integration."),
    TO_APPLICATION(
        "mapToApplication", "Map to Application Object",
        "Renames an integration record's keys to your application's fields, ready to hand back to your " +
            "application.");

    private final String actionName;
    private final String title;
    private final String description;

    FieldMappingDirection(String actionName, String title, String description) {
        this.actionName = actionName;
        this.title = title;
        this.description = description;
    }

    public String actionName() {
        return actionName;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String sourcePath(FieldMappingDescriptor.Mapping mapping) {
        return this == TO_INTEGRATION ? mapping.applicationField() : mapping.integrationField();
    }

    public String destinationPath(FieldMappingDescriptor.Mapping mapping) {
        return this == TO_INTEGRATION ? mapping.integrationField() : mapping.applicationField();
    }
}
