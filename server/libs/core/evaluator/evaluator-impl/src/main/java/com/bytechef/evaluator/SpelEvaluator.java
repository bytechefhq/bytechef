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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
 * <p>
 * <b>Security Note:</b> SpEL expression evaluation is an intentional core feature of the workflow engine. Workflow
 * creators use SpEL expressions to define data transformations, conditional logic, and dynamic value resolution within
 * their automation workflows. The SPEL_INJECTION suppression is appropriate because:
 *
 * <ul>
 * <li>Expression evaluation is the primary purpose of this component</li>
 * <li>Expressions are authored by workflow creators who have trusted access to the platform</li>
 * <li>Input validation is performed via {@code validateTextExpression} and {@code validateFormulaExpression}</li>
 * <li>Method invocation is restricted to a whitelist of safe operations via {@code methodResolver()}</li>
 * </ul>
 *
 * <p>
 * The REDOS suppression is for the regex patterns used for expression validation. These patterns are designed to be
 * safe and are applied to bounded workflow expression strings.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Mar 31, 2017
 */
@SuppressFBWarnings({
    "SPEL_INJECTION", "REDOS"
})
public class SpelEvaluator implements Evaluator {

    private static final Logger logger = LoggerFactory.getLogger(SpelEvaluator.class);

    private static final String ACCESSOR_PREFIX = "${";
    private static final String ACCESSOR_SUFFIX = "}";
    private static final String FORMULA_PREFIX = "=";
    private static final Pattern FORMULA_EXPRESSION_PATTERN =
        Pattern.compile("^(?!.*T\\()(?!.*\\.\\w+\\()(?!.*new\\s+\\w+(?:<[^>]*>)?\\s*\\[).*$");
    private static final Pattern INVALID_ACCESSOR_PATTERN = Pattern.compile(
        "\\$\\{(?!(?!T\\()[a-zA-Z_][a-zA-Z0-9_]*(?:\\[(?:\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')])*(?:\\.(?:[a-zA-Z_][a-zA-Z0-9_]*(?:\\[(?:\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')])*|\\[(?:\\d+|'[a-zA-Z0-9_\\- \\p{L}]*')]))*})");

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final Map<String, MethodExecutor> methodExecutorMap;

    private SpelEvaluator(Builder builder) {
        Map<String, MethodExecutor> map = new HashMap<>();

        map.put(EvaluatorFunctionName.ADD, new Add());
        map.put(EvaluatorFunctionName.ADD_ALL, new AddAll());
        map.put(EvaluatorFunctionName.AT_ZONE, new AtZone());
        map.put(EvaluatorFunctionName.BOOLEAN, new Cast<>(Boolean.class));
        map.put(EvaluatorFunctionName.BYTE, new Cast<>(Byte.class));
        map.put(EvaluatorFunctionName.CHAR, new Cast<>(Character.class));
        map.put(EvaluatorFunctionName.CONFIG, new Config(builder.environment));
        map.put(EvaluatorFunctionName.CONCAT, new Concat());
        map.put(EvaluatorFunctionName.CONTAINS, new Contains());
        map.put(EvaluatorFunctionName.EQUALS_IGNORE_CASE, new EqualsIgnoreCase());
        map.put(EvaluatorFunctionName.FORMAT, new Format());
        map.put(EvaluatorFunctionName.FLATTEN, new Flatten());
        map.put(EvaluatorFunctionName.FLOAT, new Cast<>(Float.class));
        map.put(EvaluatorFunctionName.DOUBLE, new Cast<>(Double.class));
        map.put(EvaluatorFunctionName.INDEX_OF, new IndexOf());
        map.put(EvaluatorFunctionName.INT, new Cast<>(Integer.class));
        map.put(EvaluatorFunctionName.JOIN, new Join());
        map.put(EvaluatorFunctionName.LAST_INDEX_OF, new LastIndexOf());
        map.put(EvaluatorFunctionName.LENGTH, new Length());
        map.put(EvaluatorFunctionName.LONG, new Cast<>(Long.class));
        map.put(EvaluatorFunctionName.MINUS_MICROS, new Minus(ChronoUnit.MICROS));
        map.put(EvaluatorFunctionName.MINUS_MILLIS, new Minus(ChronoUnit.MILLIS));
        map.put(EvaluatorFunctionName.MINUS_SECONDS, new Minus(ChronoUnit.SECONDS));
        map.put(EvaluatorFunctionName.MINUS_MINUTES, new Minus(ChronoUnit.MINUTES));
        map.put(EvaluatorFunctionName.MINUS_HOURS, new Minus(ChronoUnit.HOURS));
        map.put(EvaluatorFunctionName.MINUS_DAYS, new Minus(ChronoUnit.DAYS));
        map.put(EvaluatorFunctionName.MINUS_MONTHS, new Minus(ChronoUnit.MONTHS));
        map.put(EvaluatorFunctionName.MINUS_WEEKS, new Minus(ChronoUnit.WEEKS));
        map.put(EvaluatorFunctionName.MINUS_YEARS, new Minus(ChronoUnit.YEARS));
        map.put(EvaluatorFunctionName.NOW, new Now());
        map.put(EvaluatorFunctionName.PARSE_DATE, new Parse(Parse.Type.DATE));
        map.put(EvaluatorFunctionName.PARSE_DATE_TIME, new Parse(Parse.Type.DATE_TIME));
        map.put(EvaluatorFunctionName.PLUS_MICROS, new Plus(ChronoUnit.MICROS));
        map.put(EvaluatorFunctionName.PLUS_MILLIS, new Plus(ChronoUnit.MILLIS));
        map.put(EvaluatorFunctionName.PLUS_SECONDS, new Plus(ChronoUnit.SECONDS));
        map.put(EvaluatorFunctionName.PLUS_MINUTES, new Plus(ChronoUnit.MINUTES));
        map.put(EvaluatorFunctionName.PLUS_HOURS, new Plus(ChronoUnit.HOURS));
        map.put(EvaluatorFunctionName.PLUS_DAYS, new Plus(ChronoUnit.DAYS));
        map.put(EvaluatorFunctionName.PLUS_MONTHS, new Plus(ChronoUnit.MONTHS));
        map.put(EvaluatorFunctionName.PLUS_WEEKS, new Plus(ChronoUnit.WEEKS));
        map.put(EvaluatorFunctionName.PLUS_YEARS, new Plus(ChronoUnit.YEARS));
        map.put(EvaluatorFunctionName.PUT, new Put());
        map.put(EvaluatorFunctionName.PUT_ALL, new PutAll());
        map.put(EvaluatorFunctionName.RANGE, new Range());
        map.put(EvaluatorFunctionName.REMOVE, new Remove());
        map.put(EvaluatorFunctionName.SET, new Set());
        map.put(EvaluatorFunctionName.SIZE, new Size());
        map.put(EvaluatorFunctionName.SHORT, new Cast<>(Short.class));
        map.put(EvaluatorFunctionName.SORT, new Sort());
        map.put(EvaluatorFunctionName.SPLIT, new Split());
        map.put(EvaluatorFunctionName.SUBSTRING, new Substring());
        map.put(EvaluatorFunctionName.TIMESTAMP, new Timestamp());
        map.put(EvaluatorFunctionName.TO_MAP, new ToMap());
        map.put(EvaluatorFunctionName.UUID, new Uuid());
        map.putAll(builder.methodExecutors);

        methodExecutorMap = Collections.unmodifiableMap(map);
    }

    @Override
    public Map<String, @Nullable Object> evaluate(Map<String, ?> map, Map<String, ?> context) {
        return evaluateInternal(map, context, false);
    }

    @Override
    public Map<String, @Nullable Object> evaluate(Map<String, ?> map, Map<String, ?> context, boolean lenient) {
        return evaluateInternal(map, context, lenient);
    }

    private StandardEvaluationContext createEvaluationContext(Map<String, ?> context, boolean formulaExpression) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext(context);

        evaluationContext.addPropertyAccessor(new MapPropertyAccessor());

        if (formulaExpression) {
            evaluationContext.addMethodResolver(methodResolver());
        }

        return evaluationContext;
    }

    private String evaluate(
        CompositeStringExpression compositeStringExpression, Map<String, ?> context, boolean lenient) {

        StringBuilder sb = new StringBuilder();
        Expression[] subExpressions = compositeStringExpression.getExpressions();

        for (Expression subExpression : subExpressions) {
            if (subExpression instanceof LiteralExpression) {
                sb.append(subExpression.getValue());

                continue;
            } else if (subExpression instanceof SpelExpression) {
                sb.append(
                    evaluate(
                        ACCESSOR_PREFIX + subExpression.getExpressionString() + ACCESSOR_SUFFIX, context, lenient));

                continue;
            }

            Class<? extends Expression> subExpressionClass = subExpression.getClass();

            throw new IllegalArgumentException("unknown expression type: " + subExpressionClass.getName());
        }

        return sb.toString();
    }

    @Nullable
    private Object evaluate(@Nullable Object value, Map<String, ?> context, boolean lenient) {
        if (value == null) {
            return null;
        }

        switch (value) {
            case String string -> {
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
                            if (lenient) {
                                if (logger.isDebugEnabled()) {
                                    logger.debug("Invalid formula expression: {}", string);
                                }

                                return value;
                            }

                            throw new IllegalArgumentException("Invalid formula expression: " + string);
                        }

                        expression = expressionParser.parseExpression(string.substring(1));
                    } catch (ParseException parseException) {
                        if (lenient) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Unparseable formula expression: {}", string, parseException);
                            }

                            return value;
                        }

                        throw new IllegalArgumentException("Unparseable formula expression: " + string, parseException);
                    }
                } else {
                    if (!validateTextExpression(string)) {
                        if (lenient) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Invalid expression: {}", string);
                            }

                            return value;
                        }

                        throw new IllegalArgumentException("Invalid expression: " + string);
                    }

                    expression = expressionParser.parseExpression(
                        string, new TemplateParserContext(ACCESSOR_PREFIX, ACCESSOR_SUFFIX));
                }

                if (expression instanceof CompositeStringExpression) { // attempt partial evaluation
                    return evaluate((CompositeStringExpression) expression, context, lenient);
                } else if (expression instanceof LiteralExpression) {
                    return expression.getValue();
                } else {
                    try {
                        return expression.getValue(createEvaluationContext(context, formulaExpression));
                    } catch (SpelEvaluationException spelEvaluationException) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(spelEvaluationException.getMessage());
                        }

                        return value;
                    }
                }
            }
            case List<?> list -> {
                List<@Nullable Object> evaluatedlist = new ArrayList<>();

                for (Object item : list) {
                    evaluatedlist.add(evaluate(item, context, lenient));
                }

                return evaluatedlist;
            }
            case Map<?, ?> map -> {
                return evaluateInternal(map, context, lenient);
            }
            default -> {
            }
        }

        return value;
    }

    private Map<String, @Nullable Object> evaluateInternal(Map<?, ?> map, Map<String, ?> context, boolean lenient) {
        Map<String, @Nullable Object> newMap = new LinkedHashMap<>();

        for (Entry<?, ?> entry : map.entrySet()) {
            newMap.put((String) entry.getKey(), evaluate(entry.getValue(), context, lenient));
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
        Matcher matcher = INVALID_ACCESSOR_PATTERN.matcher(expression);

        return !matcher.find();
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
