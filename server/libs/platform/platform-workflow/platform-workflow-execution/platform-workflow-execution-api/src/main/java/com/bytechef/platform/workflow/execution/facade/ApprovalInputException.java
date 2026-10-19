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

package com.bytechef.platform.workflow.execution.facade;

import java.util.List;

/**
 * Thrown when an approval is submitted without the form inputs it requires, or with values that do not match their
 * declared types. The run stays suspended.
 *
 * @author Ivica Cardic
 */
public class ApprovalInputException extends IllegalArgumentException {

    private final List<String> errors;

    public ApprovalInputException(List<String> errors) {
        super("The approval form is incomplete: " + String.join("; ", errors) + ".");

        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
