/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.data.mapper.model;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class Pair<L, R> {

    private final L left;
    private final R right;

    public static <L, R> Pair<L, R> empty() {
        return new Pair<>(null, null);
    }

    public static <L, R> Pair<L, R> createLeft(L left) {
        return left == null ? empty() : new Pair<>(left, null);
    }

    public static <L, R> Pair<L, R> createRight(R right) {
        return right == null ? empty() : new Pair<>(null, right);
    }

    public static <L, R> Pair<L, R> create(L left, R right) {
        return right == null && left == null ? empty() : new Pair<>(left, right);
    }

    private Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }

    public int hashCode() {
        return Objects.hashCode(this.left) + 31 * Objects.hashCode(this.right);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Pair)) {
            return false;
        } else {
            Pair<L, R> pair = (Pair) obj;

            return Objects.equals(this.left, pair.left) && Objects.equals(this.right, pair.right);
        }
    }

    public String toString() {
        return "(" + this.left + ", " + this.right + ")";
    }
}
