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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Arik Cohen
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class SpelEvaluatorTest {

    private static final Evaluator EVALUATOR = SpelEvaluator.create();

    @Test
    public void test1() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "hello"), Collections.emptyMap());

        assertEquals(map, Map.of("type", "hello"));
    }

    @Test
    public void test2() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "hello", "world"), Collections.emptyMap());

        assertEquals(map, Map.of("type", "type", "hello", "world"));
    }

    @Test
    public void test3() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "${name}"), Collections.singletonMap("name", "arik"));

        assertEquals("arik", MapUtils.getString(map, "hello"));
    }

    @Test
    public void test4() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "hello", "${firstName} ${lastName}"), context);

        assertEquals("Arik Cohen", MapUtils.getString(map, "hello"));
    }

    @Test
    public void test5() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            Map<String, Object> context = new HashMap<>();

            context.put("number", "5");

            Map<String, Object> map = EVALUATOR.evaluate(
                Map.of("type", "type", "hello", "=T(java.lang.Integer).valueOf(number)"), context);

            MapUtils.get(map, "hello");
        });
    }

    @Test
    public void test6() {
        Map<String, Object> context = new HashMap<>();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "list", Arrays.asList("${firstName}", "${lastName}")), context);

        assertEquals(Arrays.asList("Arik", "Cohen"), MapUtils.getList(map, "list", String.class));
    }

    @Test
    public void test7() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "map", Collections.singletonMap("hello", "${firstName}")), context);

        assertEquals(Collections.singletonMap("hello", "Arik"), MapUtils.getMap(map, "map"));
    }

    @Test
    public void test8() {
        Map<String, Object> context = new HashMap<>();

        context.put("n1", 5);
        context.put("n2", 3);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "mult", "=n1*n2"), context);

        assertEquals(Integer.valueOf(15), MapUtils.getInteger(map, "mult"));
    }

    @Test
    public void test9() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "${name}"), Collections.emptyMap());

        assertEquals("${name}", MapUtils.getString(map, "message"));
    }

    @Test
    public void test10() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "yo ${name}"), Collections.emptyMap());

        assertEquals("yo ${name}", MapUtils.getString(map, "message"));
    }

    @Test
    public void test11() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "${number}"), Collections.singletonMap("number", 1));

        assertEquals(1, MapUtils.get(map, "thing"));
    }

    @Test
    public void test12() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "=number*3"), Collections.singletonMap("number", 1));

        assertEquals(3, MapUtils.get(map, "thing"));
    }

    @Test
    public void test13() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "thing", "=number*3"), Collections.emptyMap());

        assertEquals("=number*3", MapUtils.get(map, "thing"));
    }

    @Test
    public void test14() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "list", "=range(1,3)"), Collections.emptyMap());

        assertEquals(Arrays.asList(1, 2, 3), MapUtils.get(map, "list"));
    }

    @Test
    public void test15() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sub", Collections.singletonMap("list", "=range(1,3)")), Collections.emptyMap());

        Map<String, ?> sub = MapUtils.getRequiredMap(map, "sub");

        assertEquals(Arrays.asList(1, 2, 3), sub.get("list"));
    }

    @Test
    public void test16() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "message", "${item1}-${item2}"), Map.of("item1", "hello", "item2", "world"));

        assertEquals("hello-world", MapUtils.get(map, "message"));
    }

    @Test
    public void test17() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someBoolean", "=boolean('1')"), Collections.emptyMap());

        assertEquals(Boolean.TRUE, MapUtils.get(map, "someBoolean"));
    }

    @Test
    public void test18() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someByte", "=byte('127')"), Collections.emptyMap());

        assertEquals(Byte.MAX_VALUE, MapUtils.get(map, "someByte"));
    }

    @Test
    public void test19() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someChar", "=char('c')"), Collections.emptyMap());

        assertEquals('c', MapUtils.get(map, "someChar"));
    }

    @Test
    public void test20() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someShort", "=short('32767')"), Collections.emptyMap());

        assertEquals(Short.MAX_VALUE, MapUtils.get(map, "someShort"));
    }

    @Test
    public void test21() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someInt", "=int('1')"), Collections.emptyMap());

        assertEquals(1, MapUtils.get(map, "someInt"));
    }

    @Test
    public void test22() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someLong", "=long('1')"), Collections.emptyMap());

        assertEquals(1L, MapUtils.get(map, "someLong"));
    }

    @Test
    public void test23() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someFloat", "=float('1.337')"), Collections.emptyMap());

        assertEquals(1.337f, MapUtils.get(map, "someFloat"));
    }

    @Test
    public void test24() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "someDouble", "=double('1.337')"), Collections.emptyMap());

        assertEquals(1.337d, MapUtils.get(map, "someDouble"));
    }

    @Test
    public void test25() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(',',range(1,3))"), Collections.emptyMap());

        assertEquals("1,2,3", MapUtils.get(map, "joined"));
    }

    @Test
    public void test26() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(',',range(1,1))"), Collections.emptyMap());

        assertEquals("1", MapUtils.get(map, "joined"));
    }

    @Test
    public void test27() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "joined", "=join(' and ',{'a','b','c'})"), Collections.emptyMap());

        assertEquals("a and b and c", MapUtils.get(map, "joined"));
    }

    @Test
    public void test28() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "concatenated", "=concat({'a','b','c'}, {'d','e','f'})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "concatenated"));
    }

    @Test
    public void test29() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "concatenated", "=concat({'a','b','c'}, range(1,3))"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "concatenated"));
    }

    @Test
    public void test30() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "flattened", "=flatten({{'a','b','c'},{'d','e','f'}})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "flattened"));
    }

    @Test
    public void test31() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "flattened", "=flatten({{'a','b','c'},range(1,3)})"), Collections.emptyMap());

        assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "flattened"));
    }

//    @Test
//    public void test32() {
//        Map<String, Object> map = EVALUATOR.evaluate(
//            Map.of("type", "type", "tempDir", "=tempDir()"), Collections.emptyMap());
//
//        String tmpDir = System.getProperty("java.io.tmpdir");
//
//        if (tmpDir.endsWith(File.separator)) {
//            tmpDir = tmpDir.substring(0, tmpDir.lastIndexOf(File.separator));
//        }
//
//        assertEquals(tmpDir, MapUtils.get(map, "tempDir"));
//    }

    @Test
    public void test33() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "uuid", "=uuid()"), Collections.emptyMap());

        assertNotNull(MapUtils.get(map, "uuid"));
    }

    @Test
    public void test34() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "fullName", "${firstName} ${lastName}"), context);

        assertEquals("Arik ${lastName}", MapUtils.getString(map, "fullName"));
    }

    @Test
    public void test35() {
        Map<String, Object> context = new HashMap<>();

        context.put("num", 5.0);
        context.put("den", 10.0);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("type", "type", "result", "=num/den"), context);

        assertEquals(0.5d, MapUtils.getDouble(map, "result"));
    }

    @Test
    public void test38() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sorted", "=sort({3,1,2})"), Collections.emptyMap());

        assertEquals(Arrays.asList(1, 2, 3), MapUtils.getList(map, "sorted", Integer.class));
    }

    @Test
    public void test39() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "sorted", "=sort({'C','A','B'})"), Collections.emptyMap());

        assertEquals(Arrays.asList("A", "B", "C"), MapUtils.getList(map, "sorted", String.class));
    }

    @Test
    public void test40() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("type", "type", "date", "=format(now(),'yyyyMMdd')"), Collections.emptyMap());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        assertEquals(sdf.format(new Date()), MapUtils.getString(map, "date"));
    }

    @Test
    public void test41() {
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
    public void test42() {
        LocalDateTime localDateTime = LocalDateTime.now();

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            EVALUATOR.evaluate(Map.of("hour", "=${localDateTime}.getHour()"), Map.of("localDateTime", localDateTime));
        });

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            EVALUATOR.evaluate(Map.of("hour", "=${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime));
        });

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            EVALUATOR.evaluate(Map.of("hour", "=${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime));
        });

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            EVALUATOR.evaluate(Map.of("hour", "${localDateTime.getHour()}"), Map.of("localDateTime", localDateTime));
        });
    }

    @Test
    public void test43() {
        Map<String, Object> map = EVALUATOR.evaluate(Map.of("size", "=size(${list})"), Map.of("list", List.of(1, 2)));

        assertEquals(2, MapUtils.getInteger(map, "size"));
    }

    @Test
    public void test44() {
        Map<String, Object> testMap = new HashMap<>();

        testMap.put("list", null);

        Map<String, Object> map = EVALUATOR.evaluate(Map.of("size", "=size(${list})"), testMap);

        assertEquals(-1, MapUtils.getInteger(map, "size"));
    }

    @Test
    public void test45() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
            Map.of("str1", "test", "str2", "Test"));

        assertTrue(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    public void test46() {
        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
            Map.of("str1", "test", "str2", "abc"));

        assertFalse(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    public void test47() {
        Map<String, Object> testMap = new HashMap<>();

        testMap.put("str1", null);
        testMap.put("str2", "abc");

        Map<String, Object> map = EVALUATOR.evaluate(
            Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"), testMap);

        assertFalse(MapUtils.getBoolean(map, "equalsIgnoreCase"));
    }

    @Test
    public void test48() {
        IllegalArgumentException illegalArgumentException = assertThrowsExactly(
            IllegalArgumentException.class,
            () -> EVALUATOR.evaluate(
                Map.of("equalsIgnoreCase", "=equalsIgnoreCase(${str1}, ${str2})"),
                Map.of("str1", 6, "str2", "abc")));

        assertEquals("Invalid arguments for equalsIgnoreCase.", illegalArgumentException.getMessage());
    }
}
