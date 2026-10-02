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

package com.bytechef.platform.ai.sensitivedata;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("REDOS")
public final class SecretPatternCatalog {

    public static final List<SecretPattern> ALL = List.of(
        new SecretPattern(
            "PEM_PRIVATE_KEY",
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
            0.9),
        new SecretPattern(
            "AWS_ACCESS_KEY",
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
            0.9),
        new SecretPattern(
            "AWS_SECRET_KEY",
            Pattern.compile("(?i)aws.{0,20}?[\"'][0-9a-zA-Z/+]{40}[\"']"),
            0.6),
        new SecretPattern(
            "GITHUB_PAT",
            Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36}\\b"),
            0.9),
        new SecretPattern(
            "GITHUB_FINE_GRAINED_PAT",
            Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{22,}\\b"),
            0.9),
        new SecretPattern(
            "SLACK_TOKEN",
            Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}\\b"),
            0.9),
        new SecretPattern(
            "STRIPE_SECRET_KEY",
            Pattern.compile("\\b(?:sk_(?:live|test)_[0-9A-Za-z]{16,}|[sr]k_live_[0-9a-zA-Z]{24})\\b"),
            0.9),
        new SecretPattern(
            "STRIPE_PUBLISHABLE_KEY",
            Pattern.compile("\\bpk_(?:live|test)_[0-9A-Za-z]{16,}\\b"),
            0.9),
        new SecretPattern(
            "GOOGLE_API_KEY",
            Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"),
            0.9),
        new SecretPattern(
            "OPENAI_KEY",
            Pattern.compile("\\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\\b"),
            0.9),
        new SecretPattern(
            "JWT",
            Pattern.compile("\\bey[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\b"),
            0.9));

    private SecretPatternCatalog() {
    }

    public record SecretPattern(String type, Pattern pattern, double score) {

        public SecretPattern {
            if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("score must be between 0.0 and 1.0, got: " + score);
            }
        }
    }
}
