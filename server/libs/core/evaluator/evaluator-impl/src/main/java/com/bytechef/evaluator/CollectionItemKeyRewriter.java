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

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.Indexer;
import org.springframework.expression.spel.ast.InlineMap;
import org.springframework.expression.spel.ast.Projection;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.ast.Selection;
import org.springframework.expression.spel.ast.VariableReference;

/**
 * @author Ivica Cardic
 */
final class CollectionItemKeyRewriter {

    private static final String THIS_VARIABLE = "#this";

    private CollectionItemKeyRewriter() {
    }

    static String rewrite(String formula, SpelNode spelNode) {
        NavigableMap<Integer, Replacement> replacements = new TreeMap<>();

        collectCollectionBodies(formula, spelNode, replacements);

        if (replacements.isEmpty()) {
            return formula;
        }

        StringBuilder stringBuilder = new StringBuilder(formula);

        for (Replacement replacement : replacements.descendingMap()
            .values()) {

            stringBuilder.replace(replacement.start(), replacement.end(), replacement.text());
        }

        return stringBuilder.toString();
    }

    private static void collectCollectionBodies(
        String formula, SpelNode spelNode, Map<Integer, Replacement> replacements) {

        if (spelNode instanceof Projection || spelNode instanceof Selection) {
            collectItemKeys(formula, spelNode.getChild(0), replacements);
        }

        for (int childIndex = 0; childIndex < spelNode.getChildCount(); childIndex++) {
            collectCollectionBodies(formula, spelNode.getChild(childIndex), replacements);
        }
    }

    private static void collectItemKeys(String formula, SpelNode spelNode, Map<Integer, Replacement> replacements) {
        if (spelNode instanceof PropertyOrFieldReference propertyOrFieldReference) {
            addItemKeyLookup(propertyOrFieldReference, replacements);
        } else if (spelNode instanceof CompoundExpression compoundExpression) {
            collectChainKeys(formula, compoundExpression, replacements);
        } else if (spelNode instanceof InlineMap inlineMap) {
            for (int childIndex = 1; childIndex < inlineMap.getChildCount(); childIndex += 2) {
                collectItemKeys(formula, inlineMap.getChild(childIndex), replacements);
            }
        } else if (!(spelNode instanceof Projection || spelNode instanceof Selection)) {
            collectChildItemKeys(formula, spelNode, replacements);
        }
    }

    private static void collectChildItemKeys(
        String formula, SpelNode spelNode, Map<Integer, Replacement> replacements) {

        for (int childIndex = 0; childIndex < spelNode.getChildCount(); childIndex++) {
            collectItemKeys(formula, spelNode.getChild(childIndex), replacements);
        }
    }

    private static void collectChainKeys(
        String formula, CompoundExpression compoundExpression, Map<Integer, Replacement> replacements) {

        SpelNode firstSpelNode = compoundExpression.getChild(0);
        boolean itemRooted = true;

        if (firstSpelNode instanceof PropertyOrFieldReference propertyOrFieldReference) {
            addItemKeyLookup(propertyOrFieldReference, replacements);
        } else if (!isThisVariable(firstSpelNode)) {
            collectItemKeys(formula, firstSpelNode, replacements);

            itemRooted = false;
        }

        for (int childIndex = 1; childIndex < compoundExpression.getChildCount(); childIndex++) {
            SpelNode spelNode = compoundExpression.getChild(childIndex);

            if (itemRooted && spelNode instanceof PropertyOrFieldReference propertyOrFieldReference) {
                addChainedKeyLookup(formula, propertyOrFieldReference, replacements);
            } else if (spelNode instanceof Indexer indexer) {
                collectChildItemKeys(formula, indexer, replacements);
            } else {
                itemRooted = false;

                if (!(spelNode instanceof PropertyOrFieldReference)) {
                    collectItemKeys(formula, spelNode, replacements);
                }
            }
        }
    }

    private static void addItemKeyLookup(
        PropertyOrFieldReference propertyOrFieldReference, Map<Integer, Replacement> replacements) {

        int start = propertyOrFieldReference.getStartPosition();

        replacements.put(
            start,
            new Replacement(
                start, propertyOrFieldReference.getEndPosition(),
                THIS_VARIABLE + "['" + propertyOrFieldReference.getName() + "']"));
    }

    private static void addChainedKeyLookup(
        String formula, PropertyOrFieldReference propertyOrFieldReference, Map<Integer, Replacement> replacements) {

        int dotIndex = propertyOrFieldReference.getStartPosition() - 1;

        while (dotIndex >= 0 && Character.isWhitespace(formula.charAt(dotIndex))) {
            dotIndex--;
        }

        if (dotIndex < 0 || formula.charAt(dotIndex) != '.') {
            return;
        }

        int start = dotIndex > 0 && formula.charAt(dotIndex - 1) == '?' ? dotIndex - 1 : dotIndex;

        replacements.put(
            start,
            new Replacement(
                start, propertyOrFieldReference.getEndPosition(),
                "?.['" + propertyOrFieldReference.getName() + "']"));
    }

    private static boolean isThisVariable(SpelNode spelNode) {
        return spelNode instanceof VariableReference && THIS_VARIABLE.equals(spelNode.toStringAST());
    }

    private record Replacement(int start, int end, String text) {
    }
}
