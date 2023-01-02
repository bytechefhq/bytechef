
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
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.utils.MapUtils;
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
public class TaskEvaluatorTest {

    @Test
    public void test1() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution();
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(taskExecution.getWorkflowTask(), taskExecution.getWorkflowTask());
    }

    @Test
    public void test2() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "world"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(taskExecution.getWorkflowTask(), taskExecution.getWorkflowTask());
    }

    @Test
    public void test3() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "${name}"));
        taskExecution.evaluate(evaluator, new Context(Collections.singletonMap("name", "arik")));
        Assertions.assertEquals("arik", MapUtils.getString(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test4() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "${firstName} ${lastName}"));
        Context context = new Context();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals("Arik Cohen", MapUtils.getString(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test5() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("hello", "${T(java.lang.Integer).valueOf(number)}"));
        Context context = new Context();
        context.put("number", "5");
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Integer.valueOf(5), (Integer) MapUtils.get(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test6() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("list", Arrays.asList("${firstName}", "${lastName}")));
        Context context = new Context();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Arrays.asList("Arik", "Cohen"),
            MapUtils.getList(taskExecution.getParameters(), "list", String.class));
    }

    @Test
    public void test7() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("map", Collections.singletonMap("hello", "${firstName}")));
        Context context = new Context();
        context.put("firstName", "Arik");
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Collections.singletonMap("hello", "Arik"),
            MapUtils.getMap(taskExecution.getParameters(), "map"));
    }

    @Test
    public void test8() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("mult", "${n1*n2}"));
        Context context = new Context();
        context.put("n1", 5);
        context.put("n2", 3);
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Integer.valueOf(15), MapUtils.getInteger(taskExecution.getParameters(), "mult"));
    }

    @Test
    public void test9() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "${name}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("${name}", MapUtils.getString(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test10() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "yo ${name}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("yo ${name}", MapUtils.getString(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test11() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number}"));
        taskExecution.evaluate(evaluator, new Context(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(1), MapUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test12() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number*3}"));
        taskExecution.evaluate(evaluator, new Context(Collections.singletonMap("number", 1)));
        Assertions.assertEquals(Integer.valueOf(3), MapUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test13() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number*3}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("${number*3}", MapUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test14() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("list", "${range(1,3)}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), MapUtils.get(taskExecution.getParameters(), "list"));
    }

    @Test
    public void test15() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("sub", Collections.singletonMap("list", "${range(1,3)}")));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Arrays.asList(1, 2, 3),
            MapUtils.getMap(taskExecution.getParameters(), "sub")
                .get("list"));
    }

    @Test
    public void test16() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "${item1}-${item2}"));
        taskExecution.evaluate(evaluator, new Context(Map.of("item1", "hello", "item2", "world")));
        Assertions.assertEquals("hello-world", MapUtils.get(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test17() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someBoolean", "${boolean('1')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Boolean.TRUE, MapUtils.get(taskExecution.getParameters(), "someBoolean"));
    }

    @Test
    public void test18() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someByte", "${byte('127')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Byte.valueOf(Byte.MAX_VALUE), MapUtils.get(taskExecution.getParameters(), "someByte"));
    }

    @Test
    public void test19() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someChar", "${char('c')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Character.valueOf('c'), MapUtils.get(taskExecution.getParameters(), "someChar"));
    }

    @Test
    public void test20() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someShort", "${short('32767')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Short.valueOf(Short.MAX_VALUE), MapUtils.get(taskExecution.getParameters(), "someShort"));
    }

    @Test
    public void test21() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someInt", "${int('1')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Integer.valueOf(1), MapUtils.get(taskExecution.getParameters(), "someInt"));
    }

    @Test
    public void test22() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someLong", "${long('1')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(Long.valueOf(1L), MapUtils.get(taskExecution.getParameters(), "someLong"));
    }

    @Test
    public void test23() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someFloat", "${float('1.337')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Float.valueOf(1.337f), MapUtils.get(taskExecution.getParameters(), "someFloat"));
    }

    @Test
    public void test24() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someDouble", "${double('1.337')}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Double.valueOf(1.337d), MapUtils.get(taskExecution.getParameters(), "someDouble"));
    }

    @Test
    public void test25() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(',',range(1,3))}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("1,2,3", MapUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test26() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(',',range(1,1))}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("1", MapUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test27() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(' and ',{'a','b','c'})}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals("a and b and c", MapUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test28() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("concatenated", "${concat({'a','b','c'}, {'d','e','f'})}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", "d", "e", "f"),
            MapUtils.get(taskExecution.getParameters(), "concatenated"));
    }

    @Test
    public void test29() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("concatenated", "${concat({'a','b','c'}, range(1,3))}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", 1, 2, 3),
            MapUtils.get(taskExecution.getParameters(), "concatenated"));
    }

    @Test
    public void test30() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("flattened", "${flatten({{'a','b','c'},{'d','e','f'}})}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", "d", "e", "f"),
            MapUtils.get(taskExecution.getParameters(), "flattened"));
    }

    @Test
    public void test31() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("flattened", "${flatten({{'a','b','c'},range(1,3)})}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", 1, 2, 3),
            MapUtils.get(taskExecution.getParameters(), "flattened"));
    }

    @Test
    public void test32() {
        TaskEvaluator evaluator = TaskEvaluator.builder()
            .methodExecutor("tempDir", new TempDir())
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("tempDir", "${tempDir()}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir.endsWith(File.separator)) {
            tmpDir = FilenameUtils.getFullPathNoEndSeparator(tmpDir);
        }
        Assertions.assertEquals(tmpDir, MapUtils.get(taskExecution.getParameters(), "tempDir"));
    }

    @Test
    public void test33() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("uuid", "${uuid()}"));
        taskExecution.evaluate(evaluator, new Context(Collections.emptyMap()));
        Assertions.assertNotNull(MapUtils.get(taskExecution.getParameters(), "uuid"));
    }

    @Test
    public void test34() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("fullName", "${firstName} ${lastName}"));
        Context context = new Context();
        context.put("firstName", "Arik");
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            "Arik ${lastName}", MapUtils.getString(taskExecution.getParameters(), "fullName"));
    }

    @Test
    public void test35() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("result", "${num/den}"));
        Context context = new Context();
        context.put("num", 5.0);
        context.put("den", 10.0);
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(0.5d, MapUtils.getDouble(taskExecution.getParameters(), "result"));
    }

    @Test
    public void test36() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("number", "${stringf('%03d',5)}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals("005", MapUtils.getString(taskExecution.getParameters(), "number"));
    }

    @Test
    public void test37() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("number", "${stringf('%s %s','hello','world')}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals("hello world", MapUtils.getString(taskExecution.getParameters(), "number"));
    }

    @Test
    public void test38() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("sorted", "${sort({3,1,2})}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Arrays.asList(1, 2, 3),
            MapUtils.getList(taskExecution.getParameters(), "sorted", Integer.class));
    }

    @Test
    public void test39() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("sorted", "${sort({'C','A','B'})}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            Arrays.asList("A", "B", "C"),
            MapUtils.getList(taskExecution.getParameters(), "sorted", String.class));
    }

    @Test
    public void test40() {
        TaskEvaluator evaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("date", "${dateFormat(now(),'yyyyMMdd')}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Assertions.assertEquals(
            sdf.format(new Date()), MapUtils.getString(taskExecution.getParameters(), "date"));
    }

    @Test
    public void test41() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("my.property")).thenReturn("something");
        TaskEvaluator evaluator = TaskEvaluator.builder()
            .environment(environment)
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("myValue", "${config('my.property')}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals("something", MapUtils.getString(taskExecution.getParameters(), "myValue"));
    }

    @Test
    public void test42() {
        Environment environment = mock(Environment.class);
        TaskEvaluator evaluator = TaskEvaluator.builder()
            .environment(environment)
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("myValue", "${config('no.such.property')}"));
        Context context = new Context();
        taskExecution.evaluate(evaluator, context);
        Assertions.assertEquals(
            "${config('no.such.property')}", MapUtils.getString(taskExecution.getParameters(), "myValue"));
    }
}
