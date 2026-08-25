/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Locates high-signal developer-secret shapes: PEM private-key blocks, cloud and provider API keys, and JSON web
 * tokens. Every span is reported under the single category {@code SECRET}, so they all redact to
 * {@code [REDACTED_SECRET]} — naming the provider in the placeholder would itself disclose which service a leaked
 * credential belonged to.
 *
 * <p>
 * Each pattern is anchored, fixed-length, or bounded by a single quantifier or literal terminator, so none can be
 * driven into catastrophic backtracking. Entropy and random-string detection deliberately live elsewhere (the workflow
 * layer's {@code SecretKeyDetectorUtils}) for callers who want them.
 * </p>
 *
 * <p>
 * Regex matching is local, so this detector is stream-safe.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexSecretDetector implements SensitiveDataDetector {

    private static final String CATEGORY = "SECRET";

    private static final List<Pattern> PATTERNS = List.of(
        // PEM private-key block (redact the whole block, not just the marker)
        Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----"),
        // AWS access key id
        Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
        // GitHub personal/OAuth/app tokens (classic) and fine-grained PATs
        Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{36}\\b"),
        Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{22,}\\b"),
        // OpenAI API keys (incl. project-scoped)
        Pattern.compile("\\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\\b"),
        // Slack tokens
        Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}\\b"),
        // Stripe secret / restricted live keys
        Pattern.compile("\\b[sr]k_live_[0-9a-zA-Z]{24}\\b"),
        // Google API keys
        Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b"),
        // JSON Web Tokens (three base64url segments)
        Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"));

    @Override
    public String name() {
        return "regex-secret";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        List<SensitiveSpan> spans = new ArrayList<>();

        for (Pattern pattern : PATTERNS) {
            Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                spans.add(SensitiveSpan.of(SensitiveKind.SECRET, CATEGORY, matcher.start(), matcher.end()));
            }
        }

        return spans;
    }
}
