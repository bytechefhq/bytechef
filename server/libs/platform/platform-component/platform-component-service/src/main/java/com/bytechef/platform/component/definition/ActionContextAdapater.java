/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public class ActionContextAdapater implements ActionContext {

    private final ClusterElementContext context;

    public ActionContextAdapater(ClusterElementContext context) {
        this.context = context;
    }

    @Override
    public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> R convert(ContextFunction<Convert, R> convertFunction) {
        return context.convert(convertFunction);
    }

    @Override
    public void event(Consumer<Event> eventConsumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
        return context.encoder(encoderFunction);
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        return context.file(fileFunction);
    }

    @Override
    public <R> R http(ContextFunction<Http, R> httpFunction) {
        return context.http(httpFunction);
    }

    @Override
    public boolean isEditorEnvironment() {
        return context.isEditorEnvironment();
    }

    @Override
    public <R> R json(ContextFunction<Json, R> jsonFunction) {
        return context.json(jsonFunction);
    }

    @Override
    public void log(ContextConsumer<Log> logConsumer) {
        context.log(logConsumer);
    }

    @Override
    public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeContextFunction) {
        return context.mimeType(mimeTypeContextFunction);
    }

    @Override
    public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
        return context.outputSchema(outputSchemaFunction);
    }

    @Override
    public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
        return context.xml(xmlFunction);
    }
}
