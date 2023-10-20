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

package com.bytechef.hermes.component;

import com.bytechef.hermes.component.definition.ComponentAction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDisplay;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.definition.DisplayOption;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.PropertyOption;
import com.bytechef.hermes.definition.Resources;

/**
 * @author Ivica Cardic
 */
public final class ComponentDSL {

    public static Property.AnyProperty any() {
        return any(null);
    }

    public static Property.AnyProperty any(String name) {
        return new Property.AnyProperty(name);
    }

    public static Property.ArrayProperty array() {
        return new Property.ArrayProperty(null);
    }

    public static Property.ArrayProperty array(String name) {
        return new Property.ArrayProperty(name);
    }

    public static Property.BooleanProperty bool() {
        return new Property.BooleanProperty(null);
    }

    public static Property.BooleanProperty bool(String name) {
        return new Property.BooleanProperty(name);
    }

    public static Property.DateTimeProperty dateTime() {
        return new Property.DateTimeProperty(null);
    }

    public static Property.DateTimeProperty dateTime(String name) {
        return new Property.DateTimeProperty(name);
    }

    public static ComponentDisplay display(String label) {
        return new ComponentDisplay(label);
    }

    public static Property.ObjectProperty fileEntry() {
        return fileEntry(null);
    }

    public static Property.ObjectProperty fileEntry(String name) {
        return new Property.ObjectProperty(name)
                .properties(
                        string("extension").required(true),
                        string("mimeType").required(true),
                        string("name").required(true),
                        string("url").required(true));
    }

    public static Property.IntegerProperty integer() {
        return new Property.IntegerProperty(null);
    }

    public static Property.IntegerProperty integer(String name) {
        return new Property.IntegerProperty(name);
    }

    public static Property.NullProperty nullable() {
        return new Property.NullProperty(null);
    }

    public static Property.NullProperty nullable(String name) {
        return new Property.NullProperty(name);
    }

    public static Property.NumberProperty number() {
        return new Property.NumberProperty(null);
    }

    public static Property.NumberProperty number(String name) {
        return new Property.NumberProperty(name);
    }

    public static Property.ObjectProperty object() {
        return new Property.ObjectProperty(null);
    }

    public static Property.ObjectProperty object(String name) {
        return new Property.ObjectProperty(name);
    }

    public static ComponentAction action(String name) {
        return new ComponentAction(name);
    }

    public static Property.OptionProperty options() {
        return new Property.OptionProperty();
    }

    public static Resources resources() {
        return new Resources();
    }

    public static Property.StringProperty string() {
        return new Property.StringProperty(null);
    }

    public static Property.StringProperty string(String name) {
        return new Property.StringProperty(name);
    }

    public static ComponentDefinition createComponent(String name) {
        return new ComponentDefinition(name);
    }

    public static ConnectionDefinition createConnection(String name) {
        return new ConnectionDefinition(name);
    }

    public static DisplayOption.DisplayOptionEntry hideWhen(String propertyName) {
        return new DisplayOption.HideDisplayOptionEntry(propertyName);
    }

    public static PropertyOption option(String value) {
        return new PropertyOption(null, value, null);
    }

    public static PropertyOption option(String name, int value) {
        return new PropertyOption(name, value, null);
    }

    public static PropertyOption option(String name, String value) {
        return new PropertyOption(name, value, null);
    }

    public static PropertyOption option(String name, int value, String description) {
        return new PropertyOption(name, value, description);
    }

    public static PropertyOption option(String name, String value, String description) {
        return new PropertyOption(name, value, description);
    }

    public static DisplayOption.DisplayOptionEntry showWhen(String propertyName) {
        return new DisplayOption.ShowDisplayOptionEntry(propertyName);
    }
}
