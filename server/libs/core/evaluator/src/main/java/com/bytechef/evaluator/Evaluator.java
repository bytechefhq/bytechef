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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Task evaluator implementation which is based on Spring Expression Language for resolving expressions.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Mar 31, 2017
 */
public class Evaluator {

    private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    private static final Map<String, MethodExecutor> METHOD_EXECUTOR_MAP = new HashMap<>();
    private static final String PREFIX = "${";
    private static final String SUFFIX = "}";

    static {
        METHOD_EXECUTOR_MAP.put("boolean", new Cast<>(Boolean.class));
        METHOD_EXECUTOR_MAP.put("byte", new Cast<>(Byte.class));
        METHOD_EXECUTOR_MAP.put("char", new Cast<>(Character.class));
        METHOD_EXECUTOR_MAP.put("concat", new Concat());
        METHOD_EXECUTOR_MAP.put("dateFormat", new DateFormat());
        METHOD_EXECUTOR_MAP.put("flatten", new Flatten());
        METHOD_EXECUTOR_MAP.put("float", new Cast<>(Float.class));
        METHOD_EXECUTOR_MAP.put("double", new Cast<>(Double.class));
        METHOD_EXECUTOR_MAP.put("int", new Cast<>(Integer.class));
        METHOD_EXECUTOR_MAP.put("join", new Join());
        METHOD_EXECUTOR_MAP.put("long", new Cast<>(Long.class));
        METHOD_EXECUTOR_MAP.put("now", new Now());
        METHOD_EXECUTOR_MAP.put("range", new Range());
        METHOD_EXECUTOR_MAP.put("short", new Cast<>(Short.class));
        METHOD_EXECUTOR_MAP.put("sort", new Sort());
        METHOD_EXECUTOR_MAP.put("stringf", new StringFormat());
        METHOD_EXECUTOR_MAP.put("systemProperty", new SystemProperty());
        METHOD_EXECUTOR_MAP.put("tempDir", new TempDir());
        METHOD_EXECUTOR_MAP.put("timestamp", new Timestamp());
        METHOD_EXECUTOR_MAP.put("uuid", new Uuid());
    }

    public static Map<String, Object> evaluate(Map<String, Object> map, Map<String, ?> context) {
        return evaluateInternal(map, context);
    }

    private static StandardEvaluationContext createEvaluationContext(Map<String, ?> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext(context);

        evaluationContext.addPropertyAccessor(new MapPropertyAccessor());
        evaluationContext.addMethodResolver(methodResolver());

        return evaluationContext;
    }

    private static String evaluate(CompositeStringExpression compositeStringExpression, Map<String, ?> context) {
        StringBuilder stringBuilder = new StringBuilder();
        Expression[] subExpressions = compositeStringExpression.getExpressions();

        for (Expression subExpression : subExpressions) {
            if (subExpression instanceof LiteralExpression) {
                stringBuilder.append(subExpression.getValue());

                continue;
            } else if (subExpression instanceof SpelExpression) {
                stringBuilder.append(evaluate(PREFIX + subExpression.getExpressionString() + SUFFIX, context));

                continue;
            }

            throw new IllegalArgumentException(
                "unknown expression type: " + subExpression.getClass()
                    .getName());
        }

        return stringBuilder.toString();
    }

    private static Object evaluate(Object value, Map<String, ?> context) {
        if (value instanceof String) {
            Expression expression = EXPRESSION_PARSER.parseExpression(
                (String) value, new TemplateParserContext(PREFIX, SUFFIX));

            if (expression instanceof CompositeStringExpression) { // attempt partial evaluation
                return evaluate((CompositeStringExpression) expression, context);
            } else {
                try {
                    return expression.getValue(createEvaluationContext(context));
                } catch (SpelEvaluationException e) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(e.getMessage());
                    }

                    return value;
                }
            }
        } else if (value instanceof List<?> list) {
            List<Object> evaluatedlist = new ArrayList<>();

            for (Object item : list) {
                evaluatedlist.add(evaluate(item, context));
            }

            return evaluatedlist;
        } else if (value instanceof Map<?, ?> map) {
            return evaluateInternal(map, context);
        }

        return value;
    }

    private static Map<String, Object> evaluateInternal(Map<?, ?> map, Map<String, ?> context) {
        Map<String, Object> newMap = new HashMap<>();

        for (Entry<?, ?> entry : map.entrySet()) {
            newMap.put((String) entry.getKey(), evaluate(entry.getValue(), context));
        }

        return newMap;
    }

    private static MethodResolver methodResolver() {
        return (ctx, target, name, args) -> METHOD_EXECUTOR_MAP.get(name);
    }
}
