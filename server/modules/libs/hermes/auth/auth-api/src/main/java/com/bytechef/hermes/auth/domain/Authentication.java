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

package com.bytechef.hermes.auth.domain;

import com.bytechef.atlas.Accessor;
import java.util.Date;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface Authentication {
    Map<String, Object> asMap();

    /** Return the ID of the task auth. */
    String getId();

    /**
     * Return the time when the task auth was originally created.
     *
     * @return {@link Date}
     */
    Date getCreateTime();

    /** Return the name of the task auth. */
    String getName();

    /** Return the task auth properties. */
    Accessor getProperties();

    /** Return the task auth property value. */
    <T> T getProperty(String name);

    /** Return the task auth property value. */
    <T> T getProperty(String name, T defaultValue);

    /** Return the type of the task auth. */
    String getType();

    /**
     * Return the time when the task auth was updated.
     *
     * @return {@link Date}
     */
    Date getUpdateTime();
}
