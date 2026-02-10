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

package com.bytechef.platform.workflow.worker.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class FromAiTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.builder()
        .methodExecutor("fromAi", new FromAi())
        .build();

    @Test
    public void testFromAiWithNameOnly() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("param", "=fromAi('sifra_artikla')"), Collections.emptyMap());

        Object result = MapUtils.get(map, "param");

        assertInstanceOf(FromAiResult.class, result);

        FromAiResult fromAiResult = (FromAiResult) result;

        assertEquals("sifra_artikla", fromAiResult.name());
        assertNull(fromAiResult.description());
        assertEquals("STRING", fromAiResult.type());
        assertNull(fromAiResult.defaultValue());
    }

    @Test
    public void testFromAiWithNameAndDescription() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("param", "=fromAi('sifra_artikla', 'The product code')"), Collections.emptyMap());

        Object result = MapUtils.get(map, "param");

        assertInstanceOf(FromAiResult.class, result);

        FromAiResult fromAiResult = (FromAiResult) result;

        assertEquals("sifra_artikla", fromAiResult.name());
        assertEquals("The product code", fromAiResult.description());
        assertEquals("STRING", fromAiResult.type());
        assertNull(fromAiResult.defaultValue());
    }

    @Test
    public void testFromAiWithAllArguments() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("param", "=fromAi('price', 'Item price', 'NUMBER', 0)"), Collections.emptyMap());

        Object result = MapUtils.get(map, "param");

        assertInstanceOf(FromAiResult.class, result);

        FromAiResult fromAiResult = (FromAiResult) result;

        assertEquals("price", fromAiResult.name());
        assertEquals("Item price", fromAiResult.description());
        assertEquals("NUMBER", fromAiResult.type());
        assertEquals(0, fromAiResult.defaultValue());
    }

    @Test
    public void testFromAiNoArguments() {
        assertThrowsExactly(IllegalArgumentException.class, () -> EVALUATOR.evaluate(
            Map.of("param", "=fromAi()"), Collections.emptyMap()));
    }

}
