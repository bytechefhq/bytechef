
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

import com.bytechef.atlas.domain.TaskExecution;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.utils.MapValueUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

/**
 * @author Arik Cohen
 */
public class TaskEvaluatorTest {

    @Test
    public void test1() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution();

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(taskExecution.getWorkflowTask(), taskExecution.getWorkflowTask());
    }

    @Test
    public void test2() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "world"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(taskExecution.getWorkflowTask(), taskExecution.getWorkflowTask());
    }

    @Test
    public void test3() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "${name}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.singletonMap("name", "arik"));

        Assertions.assertEquals("arik", MapValueUtils.getString(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test4() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("hello", "${firstName} ${lastName}"));

        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals("Arik Cohen", MapValueUtils.getString(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test5() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("hello", "${T(java.lang.Integer).valueOf(number)}"));
        Map<String, Object> context = new HashMap<>();

        context.put("number", "5");

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            Integer.valueOf(5), (Integer) MapValueUtils.get(taskExecution.getParameters(), "hello"));
    }

    @Test
    public void test6() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("list", Arrays.asList("${firstName}", "${lastName}")));

        Map<String, Object> context = new HashMap<>();
        context.put("firstName", "Arik");
        context.put("lastName", "Cohen");

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            Arrays.asList("Arik", "Cohen"),
            MapValueUtils.getList(taskExecution.getParameters(), "list", String.class));
    }

    @Test
    public void test7() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("map", Collections.singletonMap("hello", "${firstName}")));
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");
        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            Collections.singletonMap("hello", "Arik"),
            MapValueUtils.getMap(taskExecution.getParameters(), "map"));
    }

    @Test
    public void test8() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("mult", "${n1*n2}"));
        Map<String, Object> context = new HashMap<>();

        context.put("n1", 5);
        context.put("n2", 3);

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            Integer.valueOf(15), MapValueUtils.getInteger(taskExecution.getParameters(), "mult"));
    }

    @Test
    public void test9() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "${name}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("${name}", MapValueUtils.getString(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test10() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "yo ${name}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("yo ${name}", MapValueUtils.getString(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test11() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.singletonMap("number", 1));

        Assertions.assertEquals(1, MapValueUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test12() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number*3}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.singletonMap("number", 1));

        Assertions.assertEquals(3, MapValueUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test13() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("thing", "${number*3}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("${number*3}", MapValueUtils.get(taskExecution.getParameters(), "thing"));
    }

    @Test
    public void test14() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("list", "${range(1,3)}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(Arrays.asList(1, 2, 3), MapValueUtils.get(taskExecution.getParameters(), "list"));
    }

    @Test
    public void test15() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("sub", Collections.singletonMap("list", "${range(1,3)}")));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList(1, 2, 3),
            MapValueUtils.getMap(taskExecution.getParameters(), "sub")
                .get("list"));
    }

    @Test
    public void test16() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("message", "${item1}-${item2}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Map.of("item1", "hello", "item2", "world"));

        Assertions.assertEquals("hello-world", MapValueUtils.get(taskExecution.getParameters(), "message"));
    }

    @Test
    public void test17() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someBoolean", "${boolean('1')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(Boolean.TRUE, MapValueUtils.get(taskExecution.getParameters(), "someBoolean"));
    }

    @Test
    public void test18() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someByte", "${byte('127')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Byte.MAX_VALUE, MapValueUtils.get(taskExecution.getParameters(), "someByte"));
    }

    @Test
    public void test19() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someChar", "${char('c')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            'c', MapValueUtils.get(taskExecution.getParameters(), "someChar"));
    }

    @Test
    public void test20() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someShort", "${short('32767')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Short.MAX_VALUE, MapValueUtils.get(taskExecution.getParameters(), "someShort"));
    }

    @Test
    public void test21() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someInt", "${int('1')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(1, MapValueUtils.get(taskExecution.getParameters(), "someInt"));
    }

    @Test
    public void test22() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someLong", "${long('1')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(1L, MapValueUtils.get(taskExecution.getParameters(), "someLong"));
    }

    @Test
    public void test23() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someFloat", "${float('1.337')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            1.337f, MapValueUtils.get(taskExecution.getParameters(), "someFloat"));
    }

    @Test
    public void test24() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("someDouble", "${double('1.337')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            1.337d, MapValueUtils.get(taskExecution.getParameters(), "someDouble"));
    }

    @Test
    public void test25() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(',',range(1,3))}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("1,2,3", MapValueUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test26() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(',',range(1,1))}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("1", MapValueUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test27() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("joined", "${join(' and ',{'a','b','c'})}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("a and b and c", MapValueUtils.get(taskExecution.getParameters(), "joined"));
    }

    @Test
    public void test28() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("concatenated", "${concat({'a','b','c'}, {'d','e','f'})}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", "d", "e", "f"),
            MapValueUtils.get(taskExecution.getParameters(), "concatenated"));
    }

    @Test
    public void test29() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("concatenated", "${concat({'a','b','c'}, range(1,3))}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", 1, 2, 3),
            MapValueUtils.get(taskExecution.getParameters(), "concatenated"));
    }

    @Test
    public void test30() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("flattened", "${flatten({{'a','b','c'},{'d','e','f'}})}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", "d", "e", "f"),
            MapValueUtils.get(taskExecution.getParameters(), "flattened"));
    }

    @Test
    public void test31() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("flattened", "${flatten({{'a','b','c'},range(1,3)})}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList("a", "b", "c", 1, 2, 3),
            MapValueUtils.get(taskExecution.getParameters(), "flattened"));
    }

    @Test
    public void test32() {
        TaskEvaluator taskEvaluator = TaskEvaluator.builder()
            .methodExecutor("tempDir", new TempDir())
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("tempDir", "${tempDir()}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        String tmpDir = System.getProperty("java.io.tmpdir");

        if (tmpDir.endsWith(File.separator)) {
            tmpDir = tmpDir.substring(0, tmpDir.lastIndexOf(File.separator));
        }

        Assertions.assertEquals(tmpDir, MapValueUtils.get(taskExecution.getParameters(), "tempDir"));
    }

    @Test
    public void test33() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("uuid", "${uuid()}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertNotNull(MapValueUtils.get(taskExecution.getParameters(), "uuid"));
    }

    @Test
    public void test34() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("fullName", "${firstName} ${lastName}"));
        Map<String, Object> context = new HashMap<>();

        context.put("firstName", "Arik");

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            "Arik ${lastName}", MapValueUtils.getString(taskExecution.getParameters(), "fullName"));
    }

    @Test
    public void test35() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("result", "${num/den}"));
        Map<String, Object> context = new HashMap<>();

        context.put("num", 5.0);
        context.put("den", 10.0);

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(0.5d, MapValueUtils.getDouble(taskExecution.getParameters(), "result"));
    }

    @Test
    public void test36() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("number", "${stringf('%03d',5)}"));
        Map<String, Object> context = new HashMap<>();

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals("005", MapValueUtils.getString(taskExecution.getParameters(), "number"));
    }

    @Test
    public void test37() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(
            WorkflowTask.of("number", "${stringf('%s %s','hello','world')}"));
        Map<String, Object> context = new HashMap<>();

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals("hello world", MapValueUtils.getString(taskExecution.getParameters(), "number"));
    }

    @Test
    public void test38() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("sorted", "${sort({3,1,2})}"));
        Map<String, Object> context = new HashMap<>();

        taskExecution = taskEvaluator.evaluate(taskExecution, context);

        Assertions.assertEquals(
            Arrays.asList(1, 2, 3),
            MapValueUtils.getList(taskExecution.getParameters(), "sorted", Integer.class));
    }

    @Test
    public void test39() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("sorted", "${sort({'C','A','B'})}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            Arrays.asList("A", "B", "C"),
            MapValueUtils.getList(taskExecution.getParameters(), "sorted", String.class));
    }

    @Test
    public void test40() {
        TaskEvaluator taskEvaluator = TaskEvaluator.create();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("date", "${dateFormat(now(),'yyyyMMdd')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        Assertions.assertEquals(
            sdf.format(new Date()), MapValueUtils.getString(taskExecution.getParameters(), "date"));
    }

    @Test
    public void test41() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("my.property")).thenReturn("something");
        TaskEvaluator taskEvaluator = TaskEvaluator.builder()
            .environment(environment)
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("myValue", "${config('my.property')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals("something", MapValueUtils.getString(taskExecution.getParameters(), "myValue"));
    }

    @Test
    public void test42() {
        Environment environment = mock(Environment.class);
        TaskEvaluator taskEvaluator = TaskEvaluator.builder()
            .environment(environment)
            .build();
        TaskExecution taskExecution = new TaskExecution(WorkflowTask.of("myValue", "${config('no.such.property')}"));

        taskExecution = taskEvaluator.evaluate(taskExecution, Collections.emptyMap());

        Assertions.assertEquals(
            "${config('no.such.property')}", MapValueUtils.getString(taskExecution.getParameters(), "myValue"));
    }
}
