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

package com.bytechef.component.script.engine;

import static com.bytechef.component.script.constant.ScriptConstants.INPUT;
import static com.bytechef.component.script.constant.ScriptConstants.SCRIPT;

import com.bytechef.component.definition.Parameters;
import java.util.Map;
import java.util.function.Function;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @author Matija Petanjek
 */
public class PolyglotEngine {

    private static final Engine engine = Engine.newBuilder()
        .build();

    @SuppressWarnings("unchecked")
    public Object execute(String languageId, Parameters inputParameters) {
        try (Context polyglotContext = Context.newBuilder()
            .engine(engine)
            .allowAllAccess(true)
            .build()) {
            polyglotContext.eval(languageId, inputParameters.getRequiredString(SCRIPT));

            Function<ProxyObject, Object> performFunction = polyglotContext.getBindings(languageId)
                .getMember("perform")
                .as(new TypeLiteral<>() {});

            return performFunction.apply(ProxyObject.fromMap((Map<String, Object>) inputParameters.getMap(INPUT)));
        }
    }
}
