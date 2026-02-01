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

package com.bytechef.component.script.datastream;

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ScriptComponentDefinition.SCRIPT;
import static com.bytechef.platform.component.definition.datastream.ItemProcessor.PROCESSOR;

import com.bytechef.component.definition.Property;
import com.bytechef.component.script.datastream.definition.ScriptClusterElementDefinition;
import com.bytechef.component.script.engine.PolyglotEngine;

/**
 * @author Ivica Cardic
 */
public class ScriptJavaItemProcessor {

    public static ScriptClusterElementDefinition of(PolyglotEngine polyglotEngine) {
        return new ScriptClusterElementDefinition(
            clusterElement("java")
                .title("Java")
                .description("Transforms data stream items using custom Java code.")
                .type(PROCESSOR)
                .properties(
                    string(SCRIPT)
                        .label("Java Code")
                        .description("Custom Java code to process items. The item is available as 'item' parameter.")
                        .controlType(Property.ControlType.CODE_EDITOR)
                        .languageId("java")
                        .defaultValue(
                            "public static Object perform(Map<String, ?> input, Context context) {\n\treturn null;\n}")
                        .required(true)),
            "java", polyglotEngine);
    }

    private ScriptJavaItemProcessor() {
    }
}
