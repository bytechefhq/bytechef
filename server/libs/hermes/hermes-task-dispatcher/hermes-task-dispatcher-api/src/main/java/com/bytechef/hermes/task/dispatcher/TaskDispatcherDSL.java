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

package com.bytechef.hermes.task.dispatcher;

import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.DisplayOption;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.PropertyOption;
import com.bytechef.hermes.definition.Resources;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;

public class TaskDispatcherDSL {

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

    public static TaskDispatcherDefinition create(String name) {
        return new TaskDispatcherDefinition(name);
    }

    public static Property.DateProperty date() {
        return new Property.DateProperty(null);
    }

    public static Property.DateProperty date(String name) {
        return new Property.DateProperty(name);
    }

    public static Property.DateTimeProperty dateTime() {
        return new Property.DateTimeProperty(null);
    }

    public static Property.DateTimeProperty dateTime(String name) {
        return new Property.DateTimeProperty(name);
    }

    public static Display display(String label) {
        return new Display(label);
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

    public static Resources resources() {
        return new Resources();
    }

    public static Property.StringProperty string() {
        return new Property.StringProperty(null);
    }

    public static Property.StringProperty string(String name) {
        return new Property.StringProperty(name);
    }

    public static DisplayOption.DisplayOptionProperty hideWhen(String propertyName) {
        return new DisplayOption.DisplayOptionProperty(new DisplayOption.HideDisplayOptionCondition(propertyName));
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

    public static DisplayOption.DisplayOptionProperty showWhen(String propertyName) {
        return new DisplayOption.DisplayOptionProperty(new DisplayOption.ShowDisplayOptionCondition(propertyName));
    }

    public static Property.ObjectProperty task() {
        return task(null);
    }

    public static Property.ObjectProperty task(String name) {
        return new Property.ObjectProperty(name)
                .description("The task or task dispatcher to use.")
                .objectType("TASK");
    }
}
