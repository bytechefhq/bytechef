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

import com.integri.atlas.engine.core.context.Context;
import com.integri.atlas.engine.core.json.JSONHelper;
import com.integri.atlas.engine.core.task.SimpleTaskExecution;
import com.integri.atlas.engine.core.task.TaskExecution;
import com.integri.atlas.engine.core.task.evaluator.TaskEvaluator;
import com.integri.atlas.engine.core.xml.XMLHelper;
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
 * a {@link TaskEvaluator} implemenation which is based on
 * Spring Expression Language for resolving expressions.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public class SpelTaskEvaluator implements TaskEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();

    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, MethodExecutor> methodExecutors;

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
        map.put("config", new Config(aBuilder.environment));
        map.put("parseJSON", new ParseJSON(aBuilder.jsonHelper));
        map.put("readXML", new ReadXML(aBuilder.xmlHelper));
        map.put("writeXML", new WriteXML(aBuilder.xmlHelper));
        map.putAll(aBuilder.methodExecutors);
        methodExecutors = Collections.unmodifiableMap(map);
    }

    public <T> T evaluate(String expression, Class<T> returnType) {
        return parser.parseExpression(expression).getValue(returnType);
    }

    @Override
    public TaskExecution evaluate(TaskExecution aJobTask, Context aContext) {
        Map<String, Object> map = aJobTask.asMap();
        Map<String, Object> newMap = evaluateInternal(map, aContext);
        return SimpleTaskExecution.of(newMap);
    }

    private Map<String, Object> evaluateInternal(Map<String, Object> aMap, Context aContext) {
        Map<String, Object> newMap = new HashMap<String, Object>();
        for (Entry<String, Object> entry : aMap.entrySet()) {
            newMap.put(entry.getKey(), evaluate(entry.getValue(), aContext));
        }
        return newMap;
    }

    private Object evaluate(Object aValue, Context aContext) {
        StandardEvaluationContext context = createEvaluationContext(aContext);
        if (aValue instanceof String) {
            Expression expr = parser.parseExpression((String) aValue, new TemplateParserContext(PREFIX, SUFFIX));
            if (expr instanceof CompositeStringExpression) { // attempt partial evaluation
                StringBuilder value = new StringBuilder();
                Expression[] subExpressions = ((CompositeStringExpression) expr).getExpressions();
                for (Expression subExpression : subExpressions) {
                    if (subExpression instanceof LiteralExpression) {
                        value.append(subExpression.getValue());
                    } else if (subExpression instanceof SpelExpression) {
                        value.append(evaluate(PREFIX + subExpression.getExpressionString() + SUFFIX, aContext));
                    } else {
                        throw new IllegalArgumentException(
                            "unknown expression type: " + subExpression.getClass().getName()
                        );
                    }
                }
                return value.toString();
            } else {
                try {
                    return (expr.getValue(context));
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

        private final Map<String, MethodExecutor> methodExecutors = new HashMap<>();
        private Environment environment = new EmptyEnvironment();
        private JSONHelper jsonHelper;
        private XMLHelper xmlHelper;

        public Builder environment(Environment aEnvironment) {
            environment = aEnvironment;
            return this;
        }

        public Builder methodExecutor(String aMethodName, MethodExecutor aMethodExecutor) {
            methodExecutors.put(aMethodName, aMethodExecutor);
            return this;
        }

        public Builder jsonHelper(JSONHelper jsonHelper) {
            this.jsonHelper = jsonHelper;

            return this;
        }

        public Builder xmlHelper(XMLHelper xmlHelper) {
            this.xmlHelper = xmlHelper;

            return this;
        }

        public SpelTaskEvaluator build() {
            return new SpelTaskEvaluator(this);
        }
    }
}
