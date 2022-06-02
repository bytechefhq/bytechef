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

package com.integri.atlas.task.auth;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SimpleTaskAuth extends MapObject implements TaskAuth {

    public SimpleTaskAuth() {
        super(Collections.emptyMap());
    }

    public SimpleTaskAuth(Map<String, Object> source) {
        super(source);
    }

    public SimpleTaskAuth(TaskAuth source) {
        super(source.asMap());
    }

    @Override
    public String getId() {
        return getString(Constants.ID);
    }

    @Override
    public Date getCreateTime() {
        return getDate(Constants.CREATE_TIME);
    }

    @Override
    public String getName() {
        return getString(Constants.NAME);
    }

    @Override
    public Accessor getProperties() {
        Map<String, Object> properties = getMap(Constants.PROPERTIES);

        return properties != null ? new MapObject(properties) : new MapObject();
    }

    @Override
    public String getType() {
        return getString(Constants.TYPE);
    }

    @Override
    public Date getUpdateTime() {
        return getDate(Constants.UPDATE_TIME);
    }

    public void setCreateTime(Date createTime) {
        set(Constants.CREATE_TIME, createTime);
    }

    public void setId(String id) {
        set(Constants.ID, id);
    }

    public void setName(String name) {
        set(Constants.NAME, name);
    }

    public void setProperties(Map<String, ?> properties) {
        set(Constants.PROPERTIES, properties);
    }

    public void setType(String type) {
        set(Constants.TYPE, type);
    }

    public void setUpdateTime(Date updateTime) {
        set(Constants.UPDATE_TIME, updateTime);
    }
}
