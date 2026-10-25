/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.evaluator;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.MethodReference;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;

/**
 * @author Ivica Cardic
 */
final class NullRootNavigationRewriter {

    private NullRootNavigationRewriter() {
    }

    static String rewrite(String expressionString, SpelNode spelNode, Map<String, ?> context) {
        if (!(context.get(Evaluator.DISABLED_TASK_NAMES) instanceof Collection<?> disabledTaskNames) ||
            disabledTaskNames.isEmpty()) {

            return expressionString;
        }

        NavigableMap<Integer, String> insertions = new TreeMap<>();

        collectInsertions(expressionString, spelNode, disabledTaskNames, insertions);

        if (insertions.isEmpty()) {
            return expressionString;
        }

        StringBuilder sb = new StringBuilder(expressionString);
        NavigableMap<Integer, String> insertionMap = insertions.descendingMap();

        for (Map.Entry<Integer, String> insertion : insertionMap.entrySet()) {
            sb.insert(insertion.getKey(), insertion.getValue());
        }

        return sb.toString();
    }

    private static void collectInsertions(
        String expressionString, SpelNode spelNode, Collection<?> disabledTaskNames, Map<Integer, String> insertions) {

        if (spelNode instanceof CompoundExpression compoundExpression &&
            isDisabledTaskRoot(compoundExpression, disabledTaskNames)) {

            for (int childIndex = 1; childIndex < compoundExpression.getChildCount(); childIndex++) {
                addNullSafeNavigation(expressionString, compoundExpression.getChild(childIndex), insertions);
            }
        }

        for (int childIndex = 0; childIndex < spelNode.getChildCount(); childIndex++) {
            collectInsertions(expressionString, spelNode.getChild(childIndex), disabledTaskNames, insertions);
        }
    }

    private static boolean isDisabledTaskRoot(CompoundExpression compoundExpression, Collection<?> disabledTaskNames) {
        if (compoundExpression.getChildCount() < 2 ||
            !(compoundExpression.getChild(0) instanceof PropertyOrFieldReference propertyOrFieldReference)) {

            return false;
        }

        return disabledTaskNames.contains(propertyOrFieldReference.getName());
    }

    private static void addNullSafeNavigation(
        String expressionString, SpelNode spelNode, Map<Integer, String> insertions) {

        int startPosition = spelNode.getStartPosition();

        if (spelNode instanceof Indexer) {
            if (!expressionString.startsWith("?.", startPosition - 2)) {
                insertions.put(startPosition, "?.");
            }
        } else if (spelNode instanceof PropertyOrFieldReference || spelNode instanceof MethodReference) {
            int dotPosition = startPosition - 1;

            if (dotPosition > 0 && expressionString.charAt(dotPosition) == '.' &&
                expressionString.charAt(dotPosition - 1) != '?') {

                insertions.put(dotPosition, "?");
            }
        }
    }
}
