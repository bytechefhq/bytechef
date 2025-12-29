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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.ParseException;
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
public class SpelEvaluator implements Evaluator {

    private static final Logger logger = LoggerFactory.getLogger(SpelEvaluator.class);

    private static final String ACCESSOR_PREFIX = "${";
    private static final String ACCESSOR_SUFFIX = "}";
    private static final String FORMULA_PREFIX = "=";
    private static final Pattern ACCESSOR_EXPRESSION_PATTERN = Pattern.compile("\\$\\{(.*?)}");
    private static final Pattern FORMULA_EXPRESSION_PATTERN =
        Pattern.compile("^(?!.*T\\()(?!.*\\.\\w+\\()(?!.*new\\s+\\w+(?:<[^>]*>)?\\s*\\[).*$");
    private static final Pattern VALID_ACCESSOR_PATTERN = Pattern.compile(
        "^(?!T\\()([a-zA-Z_][a-zA-Z0-9_]*(\\[(\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')])*(\\.(([a-zA-Z_][a-zA-Z0-9_]*)|\\[(\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')]))*(\\.([a-zA-Z_][a-zA-Z0-9_]*)(\\[(\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')])*)*$)");

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final Map<String, MethodExecutor> methodExecutorMap;

    private SpelEvaluator(Builder builder) {
        Map<String, MethodExecutor> map = new HashMap<>();

        map.put("add", new Add());
        map.put("addAll", new AddAll());
        map.put("atZone", new AtZone());
        map.put("boolean", new Cast<>(Boolean.class));
        map.put("byte", new Cast<>(Byte.class));
        map.put("char", new Cast<>(Character.class));
        map.put("config", new Config(builder.environment));
        map.put("concat", new Concat());
        map.put("contains", new Contains());
        map.put("equalsIgnoreCase", new EqualsIgnoreCase());
        map.put("format", new Format());
        map.put("flatten", new Flatten());
        map.put("float", new Cast<>(Float.class));
        map.put("double", new Cast<>(Double.class));
        map.put("indexOf", new IndexOf());
        map.put("int", new Cast<>(Integer.class));
        map.put("join", new Join());
        map.put("lastIndexOf", new LastIndexOf());
        map.put("length", new Length());
        map.put("long", new Cast<>(Long.class));
        map.put("minusMicros", new Minus(ChronoUnit.MICROS));
        map.put("minusMillis", new Minus(ChronoUnit.MILLIS));
        map.put("minusSeconds", new Minus(ChronoUnit.SECONDS));
        map.put("minusMinutes", new Minus(ChronoUnit.MINUTES));
        map.put("minusHours", new Minus(ChronoUnit.HOURS));
        map.put("minusDays", new Minus(ChronoUnit.DAYS));
        map.put("minusMonths", new Minus(ChronoUnit.MONTHS));
        map.put("minusWeeks", new Minus(ChronoUnit.WEEKS));
        map.put("minusYears", new Minus(ChronoUnit.YEARS));
        map.put("now", new Now());
        map.put("parseDate", new Parse(Parse.Type.DATE));
        map.put("parseDateTime", new Parse(Parse.Type.DATE_TIME));
        map.put("plusMicros", new Plus(ChronoUnit.MICROS));
        map.put("plusMillis", new Plus(ChronoUnit.MILLIS));
        map.put("plusSeconds", new Plus(ChronoUnit.SECONDS));
        map.put("plusMinutes", new Plus(ChronoUnit.MINUTES));
        map.put("plusHours", new Plus(ChronoUnit.HOURS));
        map.put("plusDays", new Plus(ChronoUnit.DAYS));
        map.put("plusMonths", new Plus(ChronoUnit.MONTHS));
        map.put("plusWeeks", new Plus(ChronoUnit.WEEKS));
        map.put("plusYears", new Plus(ChronoUnit.YEARS));
        map.put("put", new Put());
        map.put("putAll", new PutAll());
        map.put("range", new Range());
        map.put("remove", new Remove());
        map.put("set", new Set());
        map.put("size", new Size());
        map.put("short", new Cast<>(Short.class));
        map.put("sort", new Sort());
        map.put("split", new Split());
        map.put("substring", new Substring());
        map.put("timestamp", new Timestamp());
        map.put("toMap", new ToMap());
        map.put("uuid", new Uuid());
        map.putAll(builder.methodExecutors);

        methodExecutorMap = Collections.unmodifiableMap(map);
    }

    public Map<String, Object> evaluate(Map<String, ?> map, Map<String, ?> context) {
        return evaluateInternal(map, context);
    }

    private StandardEvaluationContext createEvaluationContext(Map<String, ?> context, boolean formulaExpression) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext(context);

        evaluationContext.addPropertyAccessor(new MapPropertyAccessor());

        if (formulaExpression) {
            evaluationContext.addMethodResolver(methodResolver());
        }

        return evaluationContext;
    }

    private String evaluate(CompositeStringExpression compositeStringExpression, Map<String, ?> context) {
        StringBuilder sb = new StringBuilder();
        Expression[] subExpressions = compositeStringExpression.getExpressions();

        for (Expression subExpression : subExpressions) {
            if (subExpression instanceof LiteralExpression) {
                sb.append(subExpression.getValue());

                continue;
            } else if (subExpression instanceof SpelExpression) {
                sb.append(evaluate(ACCESSOR_PREFIX + subExpression.getExpressionString() + ACCESSOR_SUFFIX, context));

                continue;
            }

            Class<? extends Expression> subExpressionClass = subExpression.getClass();

            throw new IllegalArgumentException("unknown expression type: " + subExpressionClass.getName());
        }

        return sb.toString();
    }

    @Nullable
    private Object evaluate(Object value, Map<String, ?> context) {
        if (value instanceof String string) {
            Expression expression;
            boolean formulaExpression = false;

            String trimmedString = string.trim();

            if (trimmedString.equals(FORMULA_PREFIX)) {
                return string;
            }

            if (trimmedString.startsWith(FORMULA_PREFIX)) {
                formulaExpression = true;

                try {
                    string = string.replaceAll("\\$\\{([^}]*)}", "$1");

                    if (!validateFormulaExpression(string)) {
                        throw new IllegalArgumentException("Invalid formula expression: " + string);
                    }

                    expression = expressionParser.parseExpression(string.substring(1));
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid formula expression: " + string, e);
                }
            } else {
                if (!validateTextExpression(string)) {
                    throw new IllegalArgumentException("Invalid expression: " + string);
                }

                expression = expressionParser.parseExpression(
                    string, new TemplateParserContext(ACCESSOR_PREFIX, ACCESSOR_SUFFIX));
            }

            if (expression instanceof CompositeStringExpression) { // attempt partial evaluation
                return evaluate((CompositeStringExpression) expression, context);
            } else if (expression instanceof LiteralExpression) {
                return expression.getValue();
            } else {
                try {
                    return expression.getValue(createEvaluationContext(context, formulaExpression));
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

    private Map<String, Object> evaluateInternal(Map<?, ?> map, Map<String, ?> context) {
        Map<String, Object> newMap = new LinkedHashMap<>();

        for (Entry<?, ?> entry : map.entrySet()) {
            newMap.put((String) entry.getKey(), evaluate(entry.getValue(), context));
        }

        return newMap;
    }

    private MethodResolver methodResolver() {
        return (ctx, target, name, args) -> {
            MethodExecutor executor = methodExecutorMap.get(name);

            if (executor == null) {
                throw new UnsupportedOperationException("Method invocation is not allowed: " + name);
            }

            return executor;
        };
    }

    private static boolean validateTextExpression(String expression) {
        Matcher accessorMatcher = ACCESSOR_EXPRESSION_PATTERN.matcher(expression);

        while (accessorMatcher.find()) {
            String accessorContent = accessorMatcher.group(1);

            Matcher validAccessorMatcher = VALID_ACCESSOR_PATTERN.matcher(accessorContent);

            if (!validAccessorMatcher.matches()) {
                return false;
            }
        }

        return true;
    }

    private static boolean validateFormulaExpression(String expression) {
        Matcher formulaMatcher = FORMULA_EXPRESSION_PATTERN.matcher(expression);

        return formulaMatcher.matches();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Evaluator create() {
        return builder().build();
    }

    public static class Builder {

        private Environment environment = new EmptyEnvironment();
        private final Map<String, MethodExecutor> methodExecutors = new HashMap<>();

        public Builder environment(Environment environment) {
            this.environment = environment;

            return this;
        }

        public Builder methodExecutor(String methodName, MethodExecutor methodExecutor) {
            methodExecutors.put(methodName, methodExecutor);

            return this;
        }

        public Evaluator build() {
            return new SpelEvaluator(this);
        }
    }
}
