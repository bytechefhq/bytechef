/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.event;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.DSL;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.uuid.UUIDGenerator;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.util.Assert;

/**
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 8, 2017
 */
public class WorkflowEvent extends MapObject implements Accessor {

    public WorkflowEvent() {
        super();
    }

    public WorkflowEvent(Map<String, Object> aSource) {
        super(new MapObject(aSource));
    }

    public String getType() {
        return getRequiredString(DSL.TYPE);
    }

    public Date getCreateTime() {
        return getDate(DSL.CREATE_TIME);
    }

    public static WorkflowEvent of(String aType) {
        return of(aType, Collections.EMPTY_MAP);
    }

    public static WorkflowEvent of(String aType, String aKey, Object aValue) {
        Assert.notNull(aKey, "key must not be null");
        Assert.notNull(aValue, "value for " + aKey + " must not be null");
        return of(aType, Map.of(aKey, aValue));
    }

    public static WorkflowEvent of(String aType, String aKey1, Object aValue1, String aKey2, Object aValue2) {
        Assert.notNull(aKey1, "key must not be null");
        Assert.notNull(aValue1, "value for " + aKey1 + " must not be null");
        Assert.notNull(aKey2, "key must not be null");
        Assert.notNull(aValue2, "value for " + aKey2 + " must not be null");
        return of(aType, Map.of(aKey1, aValue1, aKey2, aValue2));
    }

    public static WorkflowEvent of(String aType, Map<String, Object> aProperties) {
        Assert.notNull(aType, "event type must not be null");
        Map<String, Object> source = new HashMap<>(
            Map.of("id", UUIDGenerator.generate(), "type", aType, "createTime", new Date())
        );
        source.putAll(aProperties);
        return new WorkflowEvent(source);
    }
}
