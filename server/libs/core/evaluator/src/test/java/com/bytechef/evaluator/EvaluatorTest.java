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
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.evaluator;

import com.bytechef.commons.util.MapUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Arik Cohen
 */
public class EvaluatorTest {

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class MapUtilsMock extends MapUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new MapUtilsMock();
    }

    @Test
    public void test1() {
        Map<String, Object> map = Evaluator.evaluate(Map.of("type", "hello"), Collections.emptyMap());

        Assertions.assertEquals(map, Map.of("type", "hello"));
    }

    @Test
    public void test2() {
        Map<String, Object> map = Evaluator.evaluate(Map.of("type", "type", "hello", "world"), Collections.emptyMap());

        Assertions.assertEquals(map, Map.of("type", "type", "hello", "world"));
    }

    @Test
    public void test3() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "hello", "${name}"), Collections.singletonMap("name", "arik"));

        Assertions.assertEquals("arik", MapUtils.getString(map, "hello"));
    }

    @Test
    public void test4() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "hello", "${firstName} ${lastName}"), context);

        Assertions.assertEquals("Arik Cohen", MapUtils.getString(map, "hello"));
    }

    @Test
    public void test5() {
        Map<String, Object> context = new HashMap<>();

        context.put("number", "5");

        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "hello", "${T(java.lang.Integer).valueOf(number)}"), context);

        Assertions.assertEquals(Integer.valueOf(5), (Integer) MapUtils.get(map, "hello"));
    }

    @Test
    public void test6() {
        Map<String, Object> context = new HashMap<>();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "list", Arrays.asList("${firstName}", "${lastName}")), context);

        Assertions.assertEquals(Arrays.asList("Arik", "Cohen"), MapUtils.getList(map, "list", String.class));
    }

    @Test
    public void test7() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "map", Collections.singletonMap("hello", "${firstName}")), context);

        Assertions.assertEquals(Collections.singletonMap("hello", "Arik"), MapUtils.getMap(map, "map"));
    }

    @Test
    public void test8() {
        Map<String, Object> context = new HashMap<>();

        context.put("n1", 5);
        context.put("n2", 3);

        Map<String, Object> map = Evaluator.evaluate(Map.of("type", "type", "mult", "${n1*n2}"), context);

        Assertions.assertEquals(
            Integer.valueOf(15), MapUtils.getInteger(map, "mult"));
    }

    @Test
    public void test9() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "message", "${name}"), Collections.emptyMap());

        Assertions.assertEquals("${name}", MapUtils.getString(map, "message"));
    }

    @Test
    public void test10() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "message", "yo ${name}"), Collections.emptyMap());

        Assertions.assertEquals("yo ${name}", MapUtils.getString(map, "message"));
    }

    @Test
    public void test11() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "thing", "${number}"), Collections.singletonMap("number", 1));

        Assertions.assertEquals(1, MapUtils.get(map, "thing"));
    }

    @Test
    public void test12() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "thing", "${number*3}"), Collections.singletonMap("number", 1));

        Assertions.assertEquals(3, MapUtils.get(map, "thing"));
    }

    @Test
    public void test13() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "thing", "${number*3}"), Collections.emptyMap());

        Assertions.assertEquals("${number*3}", MapUtils.get(map, "thing"));
    }

    @Test
    public void test14() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "list", "${range(1,3)}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList(1, 2, 3), MapUtils.get(map, "list"));
    }

    @Test
    public void test15() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "sub", Collections.singletonMap("list", "${range(1,3)}")), Collections.emptyMap());

        Map<String, ?> sub = MapUtils.getRequiredMap(map, "sub");

        Assertions.assertEquals(Arrays.asList(1, 2, 3), sub.get("list"));
    }

    @Test
    public void test16() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "message", "${item1}-${item2}"), Map.of("item1", "hello", "item2", "world"));

        Assertions.assertEquals("hello-world", MapUtils.get(map, "message"));
    }

    @Test
    public void test17() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someBoolean", "${boolean('1')}"), Collections.emptyMap());

        Assertions.assertEquals(Boolean.TRUE, MapUtils.get(map, "someBoolean"));
    }

    @Test
    public void test18() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someByte", "${byte('127')}"), Collections.emptyMap());

        Assertions.assertEquals(Byte.MAX_VALUE, MapUtils.get(map, "someByte"));
    }

    @Test
    public void test19() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someChar", "${char('c')}"), Collections.emptyMap());

        Assertions.assertEquals('c', MapUtils.get(map, "someChar"));
    }

    @Test
    public void test20() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someShort", "${short('32767')}"), Collections.emptyMap());

        Assertions.assertEquals(Short.MAX_VALUE, MapUtils.get(map, "someShort"));
    }

    @Test
    public void test21() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someInt", "${int('1')}"), Collections.emptyMap());

        Assertions.assertEquals(1, MapUtils.get(map, "someInt"));
    }

    @Test
    public void test22() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someLong", "${long('1')}"), Collections.emptyMap());

        Assertions.assertEquals(1L, MapUtils.get(map, "someLong"));
    }

    @Test
    public void test23() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someFloat", "${float('1.337')}"), Collections.emptyMap());

        Assertions.assertEquals(1.337f, MapUtils.get(map, "someFloat"));
    }

    @Test
    public void test24() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "someDouble", "${double('1.337')}"), Collections.emptyMap());

        Assertions.assertEquals(1.337d, MapUtils.get(map, "someDouble"));
    }

    @Test
    public void test25() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "joined", "${join(',',range(1,3))}"), Collections.emptyMap());

        Assertions.assertEquals("1,2,3", MapUtils.get(map, "joined"));
    }

    @Test
    public void test26() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "joined", "${join(',',range(1,1))}"), Collections.emptyMap());

        Assertions.assertEquals("1", MapUtils.get(map, "joined"));
    }

    @Test
    public void test27() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "joined", "${join(' and ',{'a','b','c'})}"), Collections.emptyMap());

        Assertions.assertEquals("a and b and c", MapUtils.get(map, "joined"));
    }

    @Test
    public void test28() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "concatenated", "${concat({'a','b','c'}, {'d','e','f'})}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "concatenated"));
    }

    @Test
    public void test29() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "concatenated", "${concat({'a','b','c'}, range(1,3))}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "concatenated"));
    }

    @Test
    public void test30() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "flattened", "${flatten({{'a','b','c'},{'d','e','f'}})}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), MapUtils.get(map, "flattened"));
    }

    @Test
    public void test31() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "flattened", "${flatten({{'a','b','c'},range(1,3)})}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), MapUtils.get(map, "flattened"));
    }

    @Test
    public void test32() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "tempDir", "${tempDir()}"), Collections.emptyMap());

        String tmpDir = System.getProperty("java.io.tmpdir");

        if (tmpDir.endsWith(File.separator)) {
            tmpDir = tmpDir.substring(0, tmpDir.lastIndexOf(File.separator));
        }

        Assertions.assertEquals(tmpDir, MapUtils.get(map, "tempDir"));
    }

    @Test
    public void test33() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "uuid", "${uuid()}"), Collections.emptyMap());

        Assertions.assertNotNull(MapUtils.get(map, "uuid"));
    }

    @Test
    public void test34() {
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");

        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "fullName", "${firstName} ${lastName}"), context);

        Assertions.assertEquals("Arik ${lastName}", MapUtils.getString(map, "fullName"));
    }

    @Test
    public void test35() {
        Map<String, Object> context = new HashMap<>();

        context.put("num", 5.0);
        context.put("den", 10.0);

        Map<String, Object> map = Evaluator.evaluate(Map.of("type", "type", "result", "${num/den}"), context);

        Assertions.assertEquals(0.5d, MapUtils.getDouble(map, "result"));
    }

    @Test
    public void test36() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "number", "${stringf('%03d',5)}"), Collections.emptyMap());

        Assertions.assertEquals("005", MapUtils.getString(map, "number"));
    }

    @Test
    public void test37() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "number", "${stringf('%s %s','hello','world')}"), Collections.emptyMap());

        Assertions.assertEquals("hello world", MapUtils.getString(map, "number"));
    }

    @Test
    public void test38() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "sorted", "${sort({3,1,2})}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList(1, 2, 3), MapUtils.getList(map, "sorted", Integer.class));
    }

    @Test
    public void test39() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "sorted", "${sort({'C','A','B'})}"), Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList("A", "B", "C"), MapUtils.getList(map, "sorted", String.class));
    }

    @Test
    public void test40() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of("type", "type", "date", "${dateFormat(now(),'yyyyMMdd')}"), Collections.emptyMap());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Assertions.assertEquals(sdf.format(new Date()), MapUtils.getString(map, "date"));
    }

//    @Test
//    public void test41() {
//        Environment environment = mock(Environment.class);
//        when(environment.getProperty("my.property")).thenReturn("something");
//
//        Map<String, Object> map = Evaluator.evaluate(
//            Map.of("type", "type", "myValue", "${config('my.property')}"), Collections.emptyMap());
//
//        Assertions.assertEquals("something", MapValueUtils.getString(map, "myValue"));
//    }
//
//    @Test
//    public void test42() {
//        Environment environment = mock(Environment.class);
//        Map<String, Object> map = Evaluator.evaluate(
//            Map.of("type", "type", "myValue", "${config('no.such.property')}"), Collections.emptyMap());
//
//        Assertions.assertEquals(
//            "${config('no.such.property')}", MapValueUtils.getString(map, "myValue"));
//    }
}
