/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.datastream.item;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.DataStreamContext;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class DataStreamContextImpl implements DataStreamContext {

    private final ActionContext actionContext;

    public DataStreamContextImpl(ActionContext actionContext) {
        this.actionContext = actionContext;
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        return actionContext.data(actionContextData -> dataFunction.apply(
            new ActionContextDataAdapter(actionContextData)));
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        return actionContext.file(fileFunction);
    }

    @Override
    public <R> R http(ContextFunction<Http, R> httpFunction) {
        return actionContext.http(httpFunction);
    }

    @Override
    public <R> R json(ContextFunction<Json, R> jsonFunction) {
        return actionContext.json(jsonFunction);
    }

    @Override
    public void logger(ContextConsumer<Logger> loggerConsumer) {
        actionContext.logger(loggerConsumer);
    }

    @Override
    public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeFunction) {
        return actionContext.mimeType(mimeTypeFunction);
    }

    @Override
    public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
        return actionContext.outputSchema(outputSchemaFunction);
    }

    @Override
    public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
        return actionContext.xml(xmlFunction);
    }

    private record ActionContextDataAdapter(ActionContext.Data actionContextData) implements Data {

        @Override
        public <T> Optional<T> fetch(String key) {
            return actionContextData.fetch(ActionContext.Data.Scope.WORKFLOW, key);
        }

        @Override
        public <T> T get(String key) {
            return actionContextData.get(ActionContext.Data.Scope.WORKFLOW, key);
        }

        @Override
        public Void put(String key, Object data) {
            return actionContextData.put(ActionContext.Data.Scope.WORKFLOW, key, data);
        }

        @Override
        public Void remove(String key) {
            return actionContextData.remove(ActionContext.Data.Scope.WORKFLOW, key);
        }
    }
}
