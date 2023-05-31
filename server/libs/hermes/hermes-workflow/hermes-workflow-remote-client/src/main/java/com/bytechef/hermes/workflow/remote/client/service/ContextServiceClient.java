
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

package com.bytechef.hermes.workflow.remote.client.service;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.service.ContextService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextServiceClient implements ContextService {
    @Override
    public Map<String, Object> peek(long stackId, Context.Classname classname) {
        return null;
    }

    @Override
    public Map<String, Object> peek(long stackId, int subStackId, Context.Classname classname) {
        return null;
    }

    @Override
    public void push(long stackId, Context.Classname classname, Map<String, Object> value) {

    }

    @Override
    public void push(long stackId, int subStackId, Context.Classname classname, Map<String, Object> context) {

    }
}
