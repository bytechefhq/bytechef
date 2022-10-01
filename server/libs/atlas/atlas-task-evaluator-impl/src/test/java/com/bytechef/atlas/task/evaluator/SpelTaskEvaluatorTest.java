/*
 * Copyright 2016-2018 the original author or authors.
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
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.task.evaluator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.domain.Context;
import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.evaluator.spel.SpelTaskEvaluator;
import com.bytechef.atlas.task.evaluator.spel.TempDir;
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
        TaskExecution jt = new TaskExecution();
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(evaluated.getWorkflowTask(), jt.getWorkflowTask());
    }

    @Test
    public void test2() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("hello", "world");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(evaluated.getWorkflowTask(), jt.getWorkflowTask());
    }

    @Test
    public void test3() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("hello", "${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.singletonMap("name", "arik")));
        Assertions.assertEquals("arik", evaluated.getString("hello"));
    }

    @Test
    public void test4() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("hello", "${firstName} ${lastName}");
        Context ctx = new Context();
        ctx.put("firstName", "Arik");
        ctx.put("lastName", "Cohen");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("Arik Cohen", evaluated.getString("hello"));
    }

    @Test
    public void test5() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("hello", "${T(java.lang.Integer).valueOf(number)}");
        Context ctx = new Context();
        ctx.put("number", "5");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Integer.valueOf(5), ((Integer) evaluated.get("hello")));
    }

    @Test
    public void test6() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("list", Arrays.asList("${firstName}", "${lastName}"));
        Context ctx = new Context();
        ctx.put("firstName", "Arik");
        ctx.put("lastName", "Cohen");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList("Arik", "Cohen"), evaluated.getList("list", String.class));
    }

    @Test
    public void test7() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("map", Collections.singletonMap("hello", "${firstName}"));
        Context ctx = new Context();
        ctx.put("firstName", "Arik");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Collections.singletonMap("hello", "Arik"), evaluated.getMap("map"));
    }

    @Test
    public void test8() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("mult", "${n1*n2}");
        Context ctx = new Context();
        ctx.put("n1", 5);
        ctx.put("n2", 3);
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Integer.valueOf(15), evaluated.getInteger("mult"));
    }

    @Test
    public void test9() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("message", "${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("${name}", evaluated.getString("message"));
    }

    @Test
    public void test10() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("message", "yo ${name}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("yo ${name}", evaluated.getString("message"));
    }

    @Test
    public void test11() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("thing", "${number}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(1), evaluated.get("thing"));
    }

    @Test
    public void test12() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("thing", "${number*3}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(3), evaluated.get("thing"));
    }

    @Test
    public void test13() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("thing", "${number*3}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("${number*3}", evaluated.get("thing"));
    }

    @Test
    public void test14() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("list", "${range(1,3)}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.get("list"));
    }

    @Test
    public void test15() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("sub", Collections.singletonMap("list", "${range(1,3)}"));
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.getMap("sub").get("list"));
    }

    @Test
    public void test16() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("message", "${item1}-${item2}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Map.of("item1", "hello", "item2", "world")));
        Assertions.assertEquals("hello-world", evaluated.get("message"));
    }

    @Test
    public void test17() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someBoolean", "${boolean('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Boolean.valueOf(true), evaluated.get("someBoolean"));
    }

    @Test
    public void test18() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someByte", "${byte('127')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Byte.valueOf(Byte.MAX_VALUE), evaluated.get("someByte"));
    }

    @Test
    public void test19() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someChar", "${char('c')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Character.valueOf('c'), evaluated.get("someChar"));
    }

    @Test
    public void test20() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someShort", "${short('32767')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Short.valueOf(Short.MAX_VALUE), evaluated.get("someShort"));
    }

    @Test
    public void test21() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someInt", "${int('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Integer.valueOf(1), evaluated.get("someInt"));
    }

    @Test
    public void test22() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someLong", "${long('1')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Long.valueOf(1L), evaluated.get("someLong"));
    }

    @Test
    public void test23() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someFloat", "${float('1.337')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Float.valueOf(1.337f), evaluated.get("someFloat"));
    }

    @Test
    public void test24() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("someDouble", "${double('1.337')}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Double.valueOf(1.337d), evaluated.get("someDouble"));
    }

    @Test
    public void test25() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("joined", "${join(',',range(1,3))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("1,2,3", evaluated.get("joined"));
    }

    @Test
    public void test26() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("joined", "${join(',',range(1,1))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("1", evaluated.get("joined"));
    }

    @Test
    public void test27() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("joined", "${join(' and ',{'a','b','c'})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals("a and b and c", evaluated.get("joined"));
    }

    @Test
    public void test28() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("concatenated", "${concat({'a','b','c'}, {'d','e','f'})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), evaluated.get("concatenated"));
    }

    @Test
    public void test29() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("concatenated", "${concat({'a','b','c'}, range(1,3))}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), evaluated.get("concatenated"));
    }

    @Test
    public void test30() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("flattened", "${flatten({{'a','b','c'},{'d','e','f'}})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f"), evaluated.get("flattened"));
    }

    @Test
    public void test31() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("flattened", "${flatten({{'a','b','c'},range(1,3)})}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", 1, 2, 3), evaluated.get("flattened"));
    }

    @Test
    public void test32() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.builder()
                .methodExecutor("tempDir", new TempDir())
                .build();
        TaskExecution jt = TaskExecution.of("tempDir", "${tempDir()}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir.endsWith(File.separator)) {
            tmpDir = FilenameUtils.getFullPathNoEndSeparator(tmpDir);
        }
        Assertions.assertEquals(tmpDir, evaluated.get("tempDir"));
    }

    @Test
    public void test33() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("uuid", "${uuid()}");
        TaskExecution evaluated = evaluator.evaluate(jt, new Context(Collections.emptyMap()));
        Assertions.assertNotNull(evaluated.get("uuid"));
    }

    @Test
    public void test34() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("fullName", "${firstName} ${lastName}");
        Context ctx = new Context();
        ctx.put("firstName", "Arik");
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("Arik ${lastName}", evaluated.getString("fullName"));
    }

    @Test
    public void test35() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("result", "${num/den}");
        Context ctx = new Context();
        ctx.put("num", 5.0);
        ctx.put("den", 10.0);
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(0.5d, evaluated.getDouble("result"));
    }

    @Test
    public void test36() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("number", "${stringf('%03d',5)}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("005", evaluated.getString("number"));
    }

    @Test
    public void test37() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("number", "${stringf('%s %s','hello','world')}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("hello world", evaluated.getString("number"));
    }

    @Test
    public void test38() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("sorted", "${sort({3,1,2})}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), evaluated.getList("sorted", Integer.class));
    }

    @Test
    public void test39() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("sorted", "${sort({'C','A','B'})}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals(Arrays.asList("A", "B", "C"), evaluated.getList("sorted", String.class));
    }

    @Test
    public void test40() {
        SpelTaskEvaluator evaluator = SpelTaskEvaluator.create();
        TaskExecution jt = TaskExecution.of("date", "${dateFormat(now(),'yyyyMMdd')}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Assertions.assertEquals(sdf.format(new Date()), evaluated.getString("date"));
    }

    @Test
    public void test41() {
        Environment env = mock(Environment.class);
        when(env.getProperty("my.property")).thenReturn("something");
        SpelTaskEvaluator evaluator =
                SpelTaskEvaluator.builder().environment(env).build();
        TaskExecution jt = TaskExecution.of("myValue", "${config('my.property')}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("something", evaluated.getString("myValue"));
    }

    @Test
    public void test42() {
        Environment env = mock(Environment.class);
        SpelTaskEvaluator evaluator =
                SpelTaskEvaluator.builder().environment(env).build();
        TaskExecution jt = TaskExecution.of("myValue", "${config('no.such.property')}");
        Context ctx = new Context();
        TaskExecution evaluated = evaluator.evaluate(jt, ctx);
        Assertions.assertEquals("${config('no.such.property')}", evaluated.getString("myValue"));
    }
}
