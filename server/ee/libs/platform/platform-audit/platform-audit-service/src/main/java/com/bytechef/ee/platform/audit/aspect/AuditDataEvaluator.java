/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.AuditInvocation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditDataEvaluator {

    static final String BLANK_KEY = "[blank]";

    private static final Set<String> BUILT_IN_VARIABLES = Set.of("root", "this");

    private static final String RESULT_VARIABLE = "result";

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private static final Map<String, Expression> EXPRESSIONS = new ConcurrentHashMap<>();

    private AuditDataEvaluator() {
    }

    static Evaluation evaluate(
        AuditData[] auditDataEntries, AuditInvocation auditInvocation, Set<String> reservedKeys,
        Set<String> mapperKeys) {

        if (auditDataEntries.length == 0) {
            return new Evaluation(Map.of(), List.of());
        }

        EvaluationContext evaluationContext = SimpleEvaluationContext.forReadOnlyDataBinding()
            .build();

        Map<String, @Nullable Object> arguments = auditInvocation.arguments();

        arguments.forEach(evaluationContext::setVariable);

        evaluationContext.setVariable(RESULT_VARIABLE, auditInvocation.result());

        Set<String> variableNames = new HashSet<>(arguments.keySet());

        variableNames.add(RESULT_VARIABLE);

        Map<String, String> data = new LinkedHashMap<>();
        List<Failure> failures = new ArrayList<>();
        Set<String> keys = new HashSet<>();

        for (AuditData auditData : auditDataEntries) {
            String rawKey = auditData.key();
            String rawExpressionString = auditData.value();

            String key = rawKey.strip();
            String expressionString = rawExpressionString.strip();

            if (key.isEmpty()) {
                failures.add(new Failure(BLANK_KEY, expressionString, "the key is blank"));

                continue;
            }

            if (reservedKeys.contains(key)) {
                failures.add(new Failure(key, expressionString, "the key is reserved"));

                continue;
            }

            if (mapperKeys.contains(key)) {
                failures.add(new Failure(key, expressionString, "the key is already recorded by the mapper"));

                continue;
            }

            if (!keys.add(key)) {
                failures.add(new Failure(key, expressionString, "the key is declared more than once"));

                continue;
            }

            try {
                Expression expression = EXPRESSIONS.computeIfAbsent(
                    expressionString, EXPRESSION_PARSER::parseExpression);

                String unknownVariableName = findUnknownVariableName(expression, variableNames);

                if (unknownVariableName != null) {
                    failures.add(
                        new Failure(key, expressionString, "the variable #" + unknownVariableName + " is not defined"));

                    continue;
                }

                Object value = expression.getValue(evaluationContext);

                if (value != null) {
                    data.put(key, String.valueOf(value));
                }
            } catch (RuntimeException | LinkageError | StackOverflowError throwable) {
                Class<? extends Throwable> throwableClass = throwable.getClass();

                failures.add(
                    new Failure(key, expressionString, throwableClass.getSimpleName() + ": " + throwable.getMessage()));
            }
        }

        return new Evaluation(data, failures);
    }

    private static @Nullable String findUnknownVariableName(Expression expression, Set<String> variableNames) {
        if (!(expression instanceof SpelExpression spelExpression)) {
            return null;
        }

        return findUnknownVariableName(spelExpression.getAST(), variableNames);
    }

    private static @Nullable String findUnknownVariableName(SpelNode spelNode, Set<String> variableNames) {
        if (spelNode instanceof VariableReference) {
            String variableReference = spelNode.toStringAST();

            String variableName = variableReference.substring(1);

            if (!BUILT_IN_VARIABLES.contains(variableName) && !variableNames.contains(variableName)) {
                return variableName;
            }
        }

        for (int index = 0; index < spelNode.getChildCount(); index++) {
            String unknownVariableName = findUnknownVariableName(spelNode.getChild(index), variableNames);

            if (unknownVariableName != null) {
                return unknownVariableName;
            }
        }

        return null;
    }

    record Evaluation(Map<String, String> data, List<Failure> failures) {

        Evaluation {
            data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
            failures = List.copyOf(failures);
        }
    }

    record Failure(String key, String expression, String reason) {
    }
}
