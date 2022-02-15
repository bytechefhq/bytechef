/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.task.evaluator.spel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.core.context.MapContext;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

/**
 * @author Arik Cohen
 */
public class SpelTaskEvaluatorTest {

    @Test
    public void test1() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = new SimpleTaskExecution();
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(evaluated.asMap(), jt.asMap());
    }

    @Test
    public void test2() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("hello", "world");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(evaluated.asMap(), jt.asMap());
    }

    @Test
    public void test3() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("hello", "${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.singletonMap("name", "arik")));
        Assertions.assertEquals("arik", evaluated.getString("hello"));
    }

    @Test
    public void test4() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("hello", "${firstName} ${lastName}");
        MapContext ctx = new MapContext();
        ctx.put("firstName", "Arik");
        ctx.put("lastName", "Cohen");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("Arik Cohen", evaluated.getString("hello"));
    }

    @Test
    public void test5() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("hello", "${T(java.lang.Integer).valueOf(number)}");
        MapContext ctx = new MapContext();
        ctx.put("number", "5");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Integer.valueOf(5), ((Integer) evaluated.get("hello")));
    }

    @Test
    public void test6() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("list", Arrays.asList("${firstName}", "${lastName}"));
        MapContext ctx = new MapContext();
        ctx.put("firstName", "Arik");
        ctx.put("lastName", "Cohen");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList("Arik", "Cohen"), evaluated.getList("list", String.class));
    }

    @Test
    public void test7() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("map", Collections.singletonMap("hello", "${firstName}"));
        MapContext ctx = new MapContext();
        ctx.put("firstName", "Arik");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(MapObject.of(Collections.singletonMap("hello", "Arik")), evaluated.getMap("map"));
    }

    @Test
    public void test8() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("mult", "${n1*n2}");
        MapContext ctx = new MapContext();
        ctx.put("n1", 5);
        ctx.put("n2", 3);
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Integer.valueOf(15), evaluated.getInteger("mult"));
    }

    @Test
    public void test9() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("message", "${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("${name}", evaluated.getString("message"));
    }

    @Test
    public void test10() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("message", "yo ${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("yo ${name}", evaluated.getString("message"));
    }

    @Test
    public void test11() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("thing", "${number}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(1), evaluated.get("thing"));
    }

    @Test
    public void test12() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("thing", "${number*3}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(3), evaluated.get("thing"));
    }

    @Test
    public void test13() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("thing", "${number*3}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("${number*3}", evaluated.get("thing"));
    }

    @Test
    public void test14() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("list", "${range(1,3)}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.get("list"));
    }

    @Test
    public void test15() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("sub", Collections.singletonMap("list", "${range(1,3)}"));
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.getMap("sub").get("list"));
    }

    @Test
    public void test16() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("message", "${item1}-${item2}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Map.of("item1", "hello", "item2", "world")));
        Assertions.assertEquals("hello-world", evaluated.get("message"));
    }

    @Test
    public void test17() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someBoolean", "${boolean('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Boolean.valueOf(true), evaluated.get("someBoolean"));
    }

    @Test
    public void test18() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someByte", "${byte('127')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Byte.valueOf(Byte.MAX_VALUE), evaluated.get("someByte"));
    }

    @Test
    public void test19() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someChar", "${char('c')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Character.valueOf('c'), evaluated.get("someChar"));
    }

    @Test
    public void test20() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someShort", "${short('32767')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Short.valueOf(Short.MAX_VALUE), evaluated.get("someShort"));
    }

    @Test
    public void test21() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someInt", "${int('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Integer.valueOf(1), evaluated.get("someInt"));
    }

    @Test
    public void test22() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someLong", "${long('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Long.valueOf(1L), evaluated.get("someLong"));
    }

    @Test
    public void test23() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someFloat", "${float('1.337')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Float.valueOf(1.337f), evaluated.get("someFloat"));
    }

    @Test
    public void test24() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("someDouble", "${double('1.337')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Double.valueOf(1.337d), evaluated.get("someDouble"));
    }

    @Test
    public void test25() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("joined", "${join(',',range(1,3))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("1,2,3", evaluated.get("joined"));
    }

    @Test
    public void test26() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("joined", "${join(',',range(1,1))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("1", evaluated.get("joined"));
    }

    @Test
    public void test27() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("joined", "${join(' and ',{'a','b','c'})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals("a and b and c", evaluated.get("joined"));
    }

    @Test
    public void test28() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("concatenated", "${concat({'a','b','c'}, {'d','e','f'})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), evaluated.get("concatenated"));
    }

    @Test
    public void test29() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("concatenated", "${concat({'a','b','c'}, range(1,3))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), evaluated.get("concatenated"));
    }

    @Test
    public void test30() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("flattened", "${flatten({{'a','b','c'},{'d','e','f'}})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), evaluated.get("flattened"));
    }

    @Test
    public void test31() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("flattened", "${flatten({{'a','b','c'},range(1,3)})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), evaluated.get("flattened"));
    }

    @Test
    public void test32() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.builder().methodExecutor("tempDir", new TempDir()).build();
        TaskExecution jt = SimpleTaskExecution.of("tempDir", "${tempDir()}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir.endsWith(File.separator)) {
            tmpDir = FilenameUtils.getFullPathNoEndSeparator(tmpDir);
        }
        Assertions.assertEquals(tmpDir, evaluated.get("tempDir"));
    }

    @Test
    public void test33() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("uuid", "${uuid()}");
        TaskExecution evaluated = evaluator.evaluate(jt, new MapContext(Collections.emptyMap()));
        Assertions.assertNotNull(evaluated.get("uuid"));
    }

    @Test
    public void test34() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("fullName", "${firstName} ${lastName}");
        MapContext ctx = new MapContext();
        ctx.put("firstName", "Arik");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("Arik ${lastName}", evaluated.getString("fullName"));
    }

    @Test
    public void test35() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("result", "${num/den}");
        MapContext ctx = new MapContext();
        ctx.put("num", 5.0);
        ctx.put("den", 10.0);
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(0.5d, evaluated.getDouble("result"));
    }

    @Test
    public void test36() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("number", "${stringf('%03d',5)}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("005", evaluated.getString("number"));
    }

    @Test
    public void test37() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("number", "${stringf('%s %s','hello','world')}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("hello world", evaluated.getString("number"));
    }

    @Test
    public void test38() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("sorted", "${sort({3,1,2})}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.getList("sorted", Integer.class));
    }

    @Test
    public void test39() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("sorted", "${sort({'C','A','B'})}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList("A", "B", "C"), evaluated.getList("sorted", String.class));
    }

    @Test
    public void test40() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = SimpleTaskExecution.of("date", "${dateFormat(now(),'yyyyMMdd')}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Assertions.assertEquals(sdf.format(new Date()), evaluated.getString("date"));
    }

    @Test
    public void test41() {
        Environment env = mock(Environment.class);
        when(env.getProperty("my.property")).thenReturn("something");
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.builder().environment(env).build();
        TaskExecution jt = SimpleTaskExecution.of("myValue", "${config('my.property')}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("something", evaluated.getString("myValue"));
    }

    @Test
    public void test42() {
        Environment env = mock(Environment.class);
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.builder().environment(env).build();
        TaskExecution jt = SimpleTaskExecution.of("myValue", "${config('no.such.property')}");
        MapContext ctx = new MapContext();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("${config('no.such.property')}", evaluated.getString("myValue"));
    }
}
