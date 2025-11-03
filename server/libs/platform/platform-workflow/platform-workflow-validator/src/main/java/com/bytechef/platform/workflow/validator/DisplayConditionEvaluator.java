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

package com.bytechef.platform.workflow.validator;

import com.fasterxml.jackson.databind.JsonNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles evaluation of display conditions for property visibility.
 *
 * @author Marko Kriskovic
 */
class DisplayConditionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(DisplayConditionEvaluator.class);

    private DisplayConditionEvaluator() {
    }

    /**
     * Evaluates a display condition and returns the result along with any validation messages.
     */
    static DisplayConditionResult evaluate(
        @Nullable String displayCondition, JsonNode parametersJsonNode) {

        if (displayCondition == null || displayCondition.isEmpty()) {
            return DisplayConditionResult.visible();
        }

        try {
            boolean shouldShow = WorkflowUtils.extractAndEvaluateCondition(displayCondition, parametersJsonNode);
            return DisplayConditionResult.of(shouldShow, false, null);
        } catch (Exception e) {
            return handleEvaluationException(e);
        }
    }

    /**
     * Evaluates display condition for array elements with index placeholder replacement.
     */
    static DisplayConditionResult evaluateForArrayElement(
        @Nullable String displayCondition, int index, JsonNode rootParametersJsonNode) {

        if (displayCondition == null || displayCondition.isEmpty()) {
            return DisplayConditionResult.visible();
        }

        String resolvedCondition = replaceIndexPlaceholder(displayCondition, index);

        try {
            boolean shouldShow = WorkflowUtils.extractAndEvaluateCondition(resolvedCondition, rootParametersJsonNode);
            return DisplayConditionResult.of(shouldShow, false, null);
        } catch (Exception e) {
            if (logger.isTraceEnabled()) {
                logger.trace(e.getMessage());
            }
            return DisplayConditionResult.hidden();
        }
    }

    private static DisplayConditionResult handleEvaluationException(Exception e) {

        String message = e.getMessage();
        if (message != null && message.startsWith("Invalid logic for display condition:")) {
            return DisplayConditionResult.malformed(message);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(e.getMessage());
        }

        return DisplayConditionResult.hidden();
    }

    private static String replaceIndexPlaceholder(String condition, int index) {
        return condition.replace("[index]", "[" + index + "]");
    }

    /**
     * Result of display condition evaluation.
     */
    static class DisplayConditionResult {
        private final boolean shouldShow;
        private final boolean isMalformed;
        private final String malformedMessage;

        @SuppressFBWarnings("NP")
        private DisplayConditionResult(boolean shouldShow, boolean isMalformed, @Nullable String malformedMessage) {
            this.shouldShow = shouldShow;
            this.isMalformed = isMalformed;
            this.malformedMessage = malformedMessage;
        }

        static DisplayConditionResult visible() {
            return new DisplayConditionResult(true, false, null);
        }

        static DisplayConditionResult hidden() {
            return new DisplayConditionResult(false, false, null);
        }

        static DisplayConditionResult malformed(String message) {
            return new DisplayConditionResult(false, true, message);
        }

        static DisplayConditionResult of(boolean shouldShow, boolean isMalformed, @Nullable String malformedMessage) {
            return new DisplayConditionResult(shouldShow, isMalformed, malformedMessage);
        }

        boolean shouldShow() {
            return shouldShow;
        }

        boolean isMalformed() {
            return isMalformed;
        }

        String getMalformedMessage() {
            return malformedMessage;
        }
    }
}
