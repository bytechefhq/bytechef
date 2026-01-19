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

package com.bytechef.component.date.helper.constants;

import java.time.LocalDateTime;
import java.util.function.BiPredicate;

/**
 * @author Nikolina Spehar
 */
public enum DateHelperComparisonEnum {

    IS_AFTER(LocalDateTime::isAfter, "Is After"),
    IS_AFTER_OR_EQUAL(
        (a, b) -> a.isAfter(b) || a.isEqual(b), "Is After or Equal"),
    IS_BEFORE(LocalDateTime::isBefore, "Is Before"),
    IS_BEFORE_OR_EQUAL(
        (a, b) -> a.isBefore(b) || a.isEqual(b), "Is Before or Equal"),
    IS_EQUAL(LocalDateTime::isEqual, "Is Equal");

    private final BiPredicate<LocalDateTime, LocalDateTime> predicate;
    private final String label;

    DateHelperComparisonEnum(BiPredicate<LocalDateTime, LocalDateTime> predicate, String label) {
        this.predicate = predicate;
        this.label = label;
    }

    public boolean compare(LocalDateTime dateA, LocalDateTime dateB) {
        return predicate.test(dateA, dateB);
    }

    public String getLabel() {
        return label;
    }
}
