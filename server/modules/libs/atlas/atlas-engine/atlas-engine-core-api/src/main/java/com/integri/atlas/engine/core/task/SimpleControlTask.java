/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task;

import com.integri.atlas.engine.core.MapObject;

/**
 *
 * @author Arik Cohen
 * @since Apr 11, 2017
 */
public class SimpleControlTask extends MapObject implements ControlTask {

    public SimpleControlTask() {}

    public SimpleControlTask(String aType) {
        set("type", aType);
    }

    @Override
    public String getType() {
        return getString("type");
    }

    public void setType(String aType) {
        set("type", aType);
    }
}
