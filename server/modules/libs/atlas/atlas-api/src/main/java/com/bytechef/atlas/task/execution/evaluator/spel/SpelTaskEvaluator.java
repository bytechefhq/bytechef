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

package com.bytechef.atlas.task.execution.evaluator.spel;

import com.bytechef.atlas.context.domain.Context;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.atlas.task.execution.evaluator.TaskEvaluator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.common.CompositeStringExpression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * a {@link TaskEvaluator} implementation which is based on Spring Expression Language for resolving
 * expressions.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public class SpelTaskEvaluator implements TaskEvaluator {

    private final transient ExpressionParser parser = new SpelExpressionParser();

    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";

    private static final transient Logger logger = LoggerFactory.getLogger(SpelTaskEvaluator.class);

    private final transient Map<String, MethodExecutor> methodExecutors;

    private SpelTaskEvaluator(Builder aBuilder) {
        Map<String, MethodExecutor> map = new HashMap<>();

        map.put("boolean", new Cast<>(Boolean.class));
        map.put("byte", new Cast<>(Byte.class));
        map.put("char", new Cast<>(Character.class));
        map.put("short", new Cast<>(Short.class));
        map.put("int", new Cast<>(Integer.class));
        map.put("long", new Cast<>(Long.class));
        map.put("float", new Cast<>(Float.class));
        map.put("double", new Cast<>(Double.class));
        map.put("systemProperty", new SystemProperty());
        map.put("range", new Range());
        map.put("join", new Join());
        map.put("concat", new Concat());
        map.put("flatten", new Flatten());
        map.put("uuid", new Uuid());
        map.put("stringf", new StringFormat());
        map.put("sort", new Sort());
        map.put("timestamp", new Timestamp());
        map.put("now", new Now());
        map.put("dateFormat", new DateFormat());
        map.put("config", new Config(aBuilder.aEnvironment));
        map.putAll(aBuilder.methodExecutors);
        methodExecutors = Collections.unmodifiableMap(map);
    }

    @Override
    public TaskExecution evaluate(TaskExecution taskExecution, Context context) {
        Map<String, Object> map = taskExecution.asMap();
        Map<String, Object> newMap = evaluateInternal(map, context);
        return SimpleTaskExecution.of(newMap);
    }

    private Map<String, Object> evaluateInternal(Map<String, Object> aMap, Context aContext) {
        Map<String, Object> newMap = new HashMap<String, Object>();
        for (Entry<String, Object> entry : aMap.entrySet()) {
            newMap.put(entry.getKey(), evaluate(entry.getValue(), aContext));
        }
        return newMap;
    }

    private String evaluate(CompositeStringExpression compositeStringExpression, Context aContext) {
        StringBuilder stringBuilder = new StringBuilder();
        Expression[] subExpressions = compositeStringExpression.getExpressions();
        for (Expression subExpression : subExpressions) {
            if (subExpression instanceof LiteralExpression) {
                stringBuilder.append(subExpression.getValue());
                continue;
            } else if (subExpression instanceof SpelExpression) {
                stringBuilder.append(evaluate(PREFIX + subExpression.getExpressionString() + SUFFIX, aContext));
                continue;
            }
            throw new IllegalArgumentException(
                    "unknown expression type: " + subExpression.getClass().getName());
        }
        return stringBuilder.toString();
    }

    private Object evaluate(Object aValue, Context aContext) {
        if (aValue instanceof String) {
            Expression expr = parser.parseExpression((String) aValue, new TemplateParserContext(PREFIX, SUFFIX));
            if (expr instanceof CompositeStringExpression) { // attempt partial evaluation
                return evaluate((CompositeStringExpression) expr, aContext);
            } else {
                try {
                    return (expr.getValue(createEvaluationContext(aContext)));
                } catch (SpelEvaluationException e) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage());
                    }
                    return aValue;
                }
            }
        } else if (aValue instanceof List) {
            List<Object> evaluatedlist = new ArrayList<>();
            List<?> list = (List<?>) aValue;
            for (Object item : list) {
                evaluatedlist.add(evaluate(item, aContext));
            }
            return evaluatedlist;
        } else if (aValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) aValue;
            return evaluateInternal(value, aContext);
        }
        return aValue;
    }

    private StandardEvaluationContext createEvaluationContext(Context aContext) {
        StandardEvaluationContext context = new StandardEvaluationContext(aContext);
        context.addPropertyAccessor(new MapPropertyAccessor());
        context.addMethodResolver(methodResolver());
        return context;
    }

    private MethodResolver methodResolver() {
        return (ctx, target, name, args) -> {
            return methodExecutors.get(name);
        };
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SpelTaskEvaluator create() {
        return builder().build();
    }

    public static class Builder {

        private final transient Map<String, MethodExecutor> methodExecutors = new HashMap<>();
        private transient Environment aEnvironment;

        public Builder environment(Environment aEnvironment) {
            this.aEnvironment = aEnvironment;
            return this;
        }

        public Builder methodExecutor(String aMethodName, MethodExecutor aMethodExecutor) {
            methodExecutors.put(aMethodName, aMethodExecutor);
            return this;
        }

        public SpelTaskEvaluator build() {
            return new SpelTaskEvaluator(this);
        }
    }
}
