
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

package com.bytechef.error;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arik Cohen
 * @author Ivica CardFic
 * @since Apr 10, 2017
 */
public class ExecutionError {

    private String message;
    private List<String> stackTrace;

    public ExecutionError() {
    }

    public ExecutionError(String message, List<String> stackTrace) {
        this.message = message;
        this.stackTrace = new ArrayList<>(stackTrace);
    }

    /**
     * Returns the detail message string of this error.
     *
     * @return the detail message string of this {@code Error} instance (which may be {@code null}).
     */
    public String getMessage() {
        return message;
    }

    /**
     * Provides programmatic access to the stack trace information. Returns an array of stack trace elements, each
     * representing one stack frame. The zeroth element of the array (assuming the array's length is non-zero)
     * represents the top of the stack, which is the last method invocation in the sequence. Typically, this is the
     * point at which this throwable was created and thrown. The last element of the array (assuming the array's length
     * is non-zero) represents the bottom of the stack, which is the first method invocation in the sequence.
     *
     * @return an array of stack trace elements representing the stack trace pertaining to this error.
     */
    public List<String> getStackTrace() {
        return new ArrayList<>(stackTrace);
    }

    @Override
    public String toString() {
        return "Error{" + "message='" + message + '\'' + ", stackTrace=" + stackTrace + '}';
    }
}
