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

package com.bytechef.hermes.descriptor.model;

import java.util.List;

/**
 * @author Ivica Cardic
 */
public class DSL {

    public static TaskProperty.AnyTaskProperty ANY_PROPERTY() {
        return ANY_PROPERTY(null);
    }

    public static TaskProperty.AnyTaskProperty ANY_PROPERTY(String name) {
        return new TaskProperty.AnyTaskProperty(name);
    }

    public static TaskProperty.ArrayTaskProperty ARRAY_PROPERTY() {
        return new TaskProperty.ArrayTaskProperty(null);
    }

    public static TaskProperty.ArrayTaskProperty ARRAY_PROPERTY(String name) {
        return new TaskProperty.ArrayTaskProperty(name);
    }

    public static TaskProperty.BooleanTaskProperty BOOLEAN_PROPERTY() {
        return new TaskProperty.BooleanTaskProperty(null);
    }

    public static TaskProperty.BooleanTaskProperty BOOLEAN_PROPERTY(String name) {
        return new TaskProperty.BooleanTaskProperty(name);
    }

    public static TaskProperty.DateTimeTaskProperty DATE_TIME_PROPERTY() {
        return new TaskProperty.DateTimeTaskProperty(null);
    }

    public static TaskProperty.DateTimeTaskProperty DATE_TIME_PROPERTY(String name) {
        return new TaskProperty.DateTimeTaskProperty(name);
    }

    public static TaskProperty.ObjectTaskProperty FILE_ENTRY_PROPERTY() {
        return FILE_ENTRY_PROPERTY(null);
    }

    public static TaskProperty.ObjectTaskProperty FILE_ENTRY_PROPERTY(String name) {
        return new TaskProperty.ObjectTaskProperty(name)
                .properties(
                        STRING_PROPERTY("extension").required(true),
                        STRING_PROPERTY("mimeType").required(true),
                        STRING_PROPERTY("name").required(true),
                        STRING_PROPERTY("url").required(true));
    }

    public static TaskProperty.IntegerTaskProperty INTEGER_PROPERTY() {
        return new TaskProperty.IntegerTaskProperty(null);
    }

    public static TaskProperty.IntegerTaskProperty INTEGER_PROPERTY(String name) {
        return new TaskProperty.IntegerTaskProperty(name);
    }

    public static TaskProperty.NullTaskProperty NULL_PROPERTY() {
        return new TaskProperty.NullTaskProperty(null);
    }

    public static TaskProperty.NullTaskProperty NULL_PROPERTY(String name) {
        return new TaskProperty.NullTaskProperty(name);
    }

    public static TaskProperty.NumberTaskProperty NUMBER_PROPERTY() {
        return new TaskProperty.NumberTaskProperty(null);
    }

    public static TaskProperty.NumberTaskProperty NUMBER_PROPERTY(String name) {
        return new TaskProperty.NumberTaskProperty(name);
    }

    public static TaskProperty.ObjectTaskProperty OBJECT_PROPERTY() {
        return new TaskProperty.ObjectTaskProperty(null);
    }

    public static TaskProperty.ObjectTaskProperty OBJECT_PROPERTY(String name) {
        return new TaskProperty.ObjectTaskProperty(name);
    }

    public static TaskProperty.OptionTaskProperty OPTIONS() {
        return new TaskProperty.OptionTaskProperty();
    }

    public static TaskProperty.StringTaskProperty STRING_PROPERTY() {
        return new TaskProperty.StringTaskProperty(null);
    }

    public static TaskProperty.StringTaskProperty STRING_PROPERTY(String name) {
        return new TaskProperty.StringTaskProperty(name);
    }

    public static TaskOperation OPERATION(String name) {
        return new TaskOperation(name);
    }

    public static AuthenticationDescriptor createAuthenticationDescriptor(String name) {
        return new AuthenticationDescriptor(name);
    }

    public static AuthenticationDescriptors createAuthenticationDescriptors(
            String taskName, List<AuthenticationDescriptor> authenticationDescriptors) {
        return new AuthenticationDescriptors(taskName, authenticationDescriptors);
    }

    public static TaskDescriptor createTaskDescriptor(String name) {
        return new TaskDescriptor().name(name);
    }

    public static DisplayOption.DisplayOptionEntry hideWhen(String propertyName) {
        return new DisplayOption.HideDisplayOptionEntry(propertyName);
    }

    public static TaskPropertyOption option(String value) {
        return new TaskPropertyOption(null, value, null);
    }

    public static TaskPropertyOption option(String name, int value) {
        return new TaskPropertyOption(name, value, null);
    }

    public static TaskPropertyOption option(String name, String value) {
        return new TaskPropertyOption(name, value, null);
    }

    public static TaskPropertyOption option(String name, int value, String description) {
        return new TaskPropertyOption(name, value, description);
    }

    public static TaskPropertyOption option(String name, String value, String description) {
        return new TaskPropertyOption(name, value, description);
    }

    public static DisplayOption.DisplayOptionEntry showWhen(String propertyName) {
        return new DisplayOption.ShowDisplayOptionEntry(propertyName);
    }
}
