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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class SensitiveDataRedactorTest {

    private static final Set<SensitiveKind> BOTH = EnumSet.allOf(SensitiveKind.class);

    @Test
    void testAppliesNonOverlappingSpansLeftToRightInResult() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_EMAIL] [REDACTED_IP]");
    }

    @Test
    void testSecretBeatsOverlappingPii() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testLongerSpanBeatsNestedSpanOfTheSameKind() {
        SensitiveDataRedactor redactor = redactor(
            fixed("outer", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)),
            fixed("inner", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 3, 6)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_SECRET]");
    }

    @Test
    void testLongerSpanBeatsPartiallyOverlappingShorterSpanOfTheSameKind() {
        SensitiveDataRedactor redactor = redactor(
            fixed("early", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)),
            fixed("longer", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 5, 20)));

        assertThat(redactor.redact("0123456789abcdefghij", BOTH, null)).isEqualTo("01234[REDACTED_SECRET]");
    }

    @Test
    void testEarlierStartWinsWhenKindAndLengthTieAndSpansOverlap() {
        SensitiveDataRedactor redactor = redactor(
            fixed("later", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 5, 15)),
            fixed("earlier", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 10)));

        assertThat(redactor.redact("0123456789abcde", BOTH, null)).isEqualTo("[REDACTED_SECRET]abcde");
    }

    @Test
    void testEarlierCategoryWinsWhenKindLengthAndStartAllTie() {
        SensitiveDataRedactor redactor = redactor(
            fixed("beta", true, SensitiveSpan.of(SensitiveKind.SECRET, "BETA", 0, 5)),
            fixed("alpha", true, SensitiveSpan.of(SensitiveKind.SECRET, "ALPHA", 0, 5)));

        assertThat(redactor.redact("01234", BOTH, null)).isEqualTo("[REDACTED_ALPHA]");
    }

    @Test
    void testTouchingSpansBothSurvive() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true,
                SensitiveSpan.of(SensitiveKind.PII, "IP", 0, 5),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 5, 10)));

        assertThat(redactor.redact("0123456789", BOTH, null)).isEqualTo("[REDACTED_IP][REDACTED_IP]");
    }

    @Test
    void testResultIsIndependentOfDetectorOrder() {
        SensitiveDataDetector piiDetector = fixed(
            "pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21));
        SensitiveDataDetector secretDetector = fixed(
            "secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28));

        String text = "xoxb-1234567890123456-abcdef";

        List<SensitiveDataDetector> detectors = new ArrayList<>(List.of(piiDetector, secretDetector));
        String expected = new SensitiveDataRedactor(detectors).redact(text, BOTH, null);

        for (int attempt = 0; attempt < 20; attempt++) {
            Collections.shuffle(detectors);

            assertThat(new SensitiveDataRedactor(detectors).redact(text, BOTH, null)).isEqualTo(expected);
        }
    }

    @Test
    void testKindFilterIsAppliedBeforeResolutionSoAPiiOnlyCallStillRedacts() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)),
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "SECRET", 0, 28)));

        assertThat(redactor.redact("xoxb-1234567890123456-abcdef", EnumSet.of(SensitiveKind.PII), null))
            .isEqualTo("xoxb-[REDACTED_CC]-abcdef");
    }

    @Test
    void testFailingDetectorIsSkippedAndOthersStillApply() {
        RecordingMetrics metrics = new RecordingMetrics();

        SensitiveDataRedactor redactor = redactor(
            throwing("broken"),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, metrics)).isEqualTo("[REDACTED_EMAIL] def");
        assertThat(metrics.failures).containsExactly("broken");
    }

    @Test
    void testFailingDetectorWithoutMetricsDoesNotThrow() {
        SensitiveDataRedactor redactor = redactor(throwing("broken"));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc def");
    }

    @Test
    void testSpanBeyondTextIsTreatedAsDetectorFailure() {
        SensitiveDataRedactor redactor = redactor(
            fixed("rogue", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 500)),
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testDetectorReportingOneOutOfBoundsSpanDiscardsAllOfItsSpans() {
        SensitiveDataRedactor redactor = redactor(
            fixed("mixed", true,
                SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3),
                SensitiveSpan.of(SensitiveKind.PII, "IP", 0, 500)),
            fixed("valid", true, SensitiveSpan.of(SensitiveKind.PII, "SSN", 4, 7)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_SSN]");
    }

    @Test
    void testStreamSafeViewExcludesNonStreamSafeDetectors() {
        SensitiveDataRedactor redactor = redactor(
            fixed("local", true, SensitiveSpan.of(SensitiveKind.PII, "IP", 4, 7)),
            fixed("contextual", false, SensitiveSpan.of(SensitiveKind.PII, "PERSON", 0, 3)));

        assertThat(redactor.redact("abc def", BOTH, null)).isEqualTo("[REDACTED_PERSON] [REDACTED_IP]");
        assertThat(redactor.streamSafeView()
            .redact("abc def", BOTH, null)).isEqualTo("abc [REDACTED_IP]");
    }

    @Test
    void testEmptyTextIsReturnedUnchanged() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("", BOTH, null)).isEmpty();
    }

    @Test
    void testEmptyKindSetRedactsNothing() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redact("abc def", EnumSet.noneOf(SensitiveKind.class), null)).isEqualTo("abc def");
    }

    @Test
    void testDropsSpansBelowTheThreshold() {
        SensitiveDataRedactor redactor = redactor(
            fixed("weak", true, new SensitiveSpan(SensitiveKind.PII, "WEAK", 0, 3, 0.2)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, null)).isEqualTo("abc def");
    }

    @Test
    void testKeepsSpansAtOrAboveTheThreshold() {
        SensitiveDataRedactor redactor = redactor(
            fixed("strong", true, new SensitiveSpan(SensitiveKind.PII, "STRONG", 0, 3, 0.9)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, null)).isEqualTo("[REDACTED_STRONG] def");
    }

    @Test
    void testFilteringHappensBeforeResolutionSoAWeakSpanCannotWinATieBreak() {
        SensitiveDataRedactor redactor = redactor(
            fixed("weak", true, new SensitiveSpan(SensitiveKind.PII, "AAA_WEAK", 0, 3, 0.2)),
            fixed("strong", true, new SensitiveSpan(SensitiveKind.PII, "ZZZ_STRONG", 0, 3, 0.9)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, null)).isEqualTo("[REDACTED_ZZZ_STRONG] def");
    }

    @Test
    void testDetectCandidatesStaysUnfilteredForTheStreamingSafeCut() {
        SensitiveDataRedactor redactor = redactor(
            fixed("weak", true, new SensitiveSpan(SensitiveKind.PII, "WEAK", 0, 3, 0.2)));

        assertThat(redactor.detectCandidates("abc def", null)).hasSize(1);
    }

    @Test
    void testConfidenceThresholdExactlyAtTheBoundaryIsKept() {
        SensitiveDataRedactor redactor = redactor(
            fixed("boundary", true, new SensitiveSpan(SensitiveKind.PII, "BOUNDARY", 0, 3, 0.5)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, null)).isEqualTo("[REDACTED_BOUNDARY] def");
    }

    @Test
    void testRecordsBelowConfidenceThresholdWhenASpanIsDropped() {
        RecordingMetrics metrics = new RecordingMetrics();

        SensitiveDataRedactor redactor = redactor(
            fixed("weak", true, new SensitiveSpan(SensitiveKind.PII, "WEAK", 0, 3, 0.2)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, metrics)).isEqualTo("abc def");
        assertThat(metrics.belowConfidenceThresholdCount).isEqualTo(1);
    }

    @Test
    void testRecordsBelowConfidenceThresholdAtMostOncePerCallEvenWithMultipleDroppedSpans() {
        RecordingMetrics metrics = new RecordingMetrics();

        SensitiveDataRedactor redactor = redactor(
            fixed("weak", true,
                new SensitiveSpan(SensitiveKind.PII, "WEAK", 0, 3, 0.2),
                new SensitiveSpan(SensitiveKind.PII, "WEAK", 4, 7, 0.2),
                new SensitiveSpan(SensitiveKind.PII, "WEAK", 8, 11, 0.2)));

        assertThat(redactor.redact("abc def ghi", BOTH, 0.5, metrics)).isEqualTo("abc def ghi");
        assertThat(metrics.belowConfidenceThresholdCount).isEqualTo(1);
    }

    @Test
    void testSpansSurvivingTheModelFloorStillFaceThePipelineThreshold() {
        SensitiveDataRedactor redactor = redactor(
            fixed("opennlp-ner", false, new SensitiveSpan(SensitiveKind.PII, "PERSON", 0, 5, 0.55)));

        assertThat(redactor.redact("Alice went home", BOTH, 0.8, null)).isEqualTo("Alice went home");
        assertThat(redactor.redact("Alice went home", BOTH, 0.5, null)).isEqualTo("[REDACTED_PERSON] went home");
    }

    @Test
    void testDoesNotRecordBelowConfidenceThresholdWhenNothingIsDropped() {
        RecordingMetrics metrics = new RecordingMetrics();

        SensitiveDataRedactor redactor = redactor(
            fixed("strong", true, new SensitiveSpan(SensitiveKind.PII, "STRONG", 0, 3, 0.9)));

        assertThat(redactor.redact("abc def", BOTH, 0.5, metrics)).isEqualTo("[REDACTED_STRONG] def");
        assertThat(metrics.belowConfidenceThresholdCount).isZero();
    }

    @Test
    void testAnUnwindowableDetectorIsSkippedAboveTheThresholdAndTheOthersStillRun() {
        RecordingMetrics metrics = new RecordingMetrics();
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(
                fixed("windowable", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 3)),
                fixed("unwindowable", false, SensitiveSpan.of(SensitiveKind.PII, "UNWINDOWABLE", 4, 7))),
            new SensitiveDataRedactor.DetectionBounds(Duration.ofSeconds(30), 10));

        List<SensitiveSpan> spans = redactor.detectCandidates("abc def ghi jkl", metrics);

        assertThat(spans)
            .as("the windowable detectors must still cover an input the unwindowable one was skipped for")
            .anyMatch(span -> "EMAIL_ADDRESS".equals(span.category()));
        assertThat(spans).noneMatch(span -> "UNWINDOWABLE".equals(span.category()));
        assertThat(metrics.skippedOversize).containsExactly("unwindowable");
    }

    @Test
    void testTheDeadlineAbandonsRemainingWorkKeepsWhatWasFoundAndNamesTheDetector() {
        RecordingMetrics metrics = new RecordingMetrics();
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(
                fixed("windowable", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 3)),
                slowUnwindowable("slow", SensitiveSpan.of(SensitiveKind.PII, "SLOW", 4, 7)),
                fixed("after", false, SensitiveSpan.of(SensitiveKind.PII, "AFTER", 8, 11))),
            new SensitiveDataRedactor.DetectionBounds(Duration.ofMillis(50), Integer.MAX_VALUE));

        List<SensitiveSpan> spans = redactor.detectCandidates("abc def ghi jkl", metrics);

        assertThat(spans).anyMatch(span -> "EMAIL_ADDRESS".equals(span.category()));
        assertThat(spans).noneMatch(span -> "AFTER".equals(span.category()));
        assertThat(metrics.timedOut).containsExactly("after");
    }

    @Test
    void testWindowableDetectorsRunToCompletionBeforeAnUnwindowableOneCanExhaustTheBudget() {
        RecordingMetrics metrics = new RecordingMetrics();
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(
                slowUnwindowable("slow", SensitiveSpan.of(SensitiveKind.PII, "SLOW", 4, 7)),
                fixed("windowable", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 3))),
            new SensitiveDataRedactor.DetectionBounds(Duration.ofMillis(50), Integer.MAX_VALUE));

        assertThat(redactor.detectCandidates("abc def ghi jkl", metrics))
            .anyMatch(span -> "EMAIL_ADDRESS".equals(span.category()));
    }

    private static SensitiveDataRedactor redactor(SensitiveDataDetector... detectors) {
        return new SensitiveDataRedactor(List.of(detectors));
    }

    private static SensitiveDataDetector fixed(String name, boolean streamSafe, SensitiveSpan... spans) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                return List.of(spans);
            }

            @Override
            public boolean streamSafe() {
                return streamSafe;
            }
        };
    }

    private static SensitiveDataDetector slowUnwindowable(String name, SensitiveSpan... spans) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();
                }

                return List.of(spans);
            }

            @Override
            public boolean streamSafe() {
                return false;
            }
        };
    }

    private static SensitiveDataDetector throwing(String name) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw new IllegalStateException("detector is broken");
            }
        };
    }

    private static final class RecordingMetrics implements SensitiveDataMetrics {

        private final List<String> failures = new ArrayList<>();
        private final List<String> timedOut = new ArrayList<>();
        private final List<String> skippedOversize = new ArrayList<>();

        private int belowConfidenceThresholdCount;

        @Override
        public void recordDetectorFailure(String detectorName) {
            failures.add(detectorName);
        }

        @Override
        public void recordDetectorTimedOut(String detectorName) {
            timedOut.add(detectorName);
        }

        @Override
        public void recordDetectorSkippedOversize(String detectorName) {
            skippedOversize.add(detectorName);
        }

        @Override
        public void recordBelowConfidenceThreshold() {
            belowConfidenceThresholdCount++;
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testAPathologicalPatternIsCutOffAndReportedAsATimeout() {
        RecordingMetrics metrics = new RecordingMetrics();
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(
                fixed("cheap", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 3)),
                runaway("runaway", "(x+x+)+y")),
            new SensitiveDataRedactor.DetectionBounds(Duration.ofMillis(100), Integer.MAX_VALUE));

        List<SensitiveSpan> spans = redactor.detectCandidates("x".repeat(1000), metrics);

        assertThat(spans)
            .as("a cut-off detector must not cost the others their spans")
            .anyMatch(span -> "EMAIL_ADDRESS".equals(span.category()));
        assertThat(metrics.timedOut).containsExactly("runaway");
        assertThat(metrics.failures)
            .as("a match cut off mid-run is not the same fact as a detector that threw")
            .isEmpty();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testAStackOverflowInADetectorDoesNotKillThePass() {
        RecordingMetrics metrics = new RecordingMetrics();
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(
            List.of(
                fixed("cheap", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL_ADDRESS", 0, 3)),
                throwing("deep", StackOverflowError::new)),
            new SensitiveDataRedactor.DetectionBounds(Duration.ofSeconds(30), Integer.MAX_VALUE));

        List<SensitiveSpan> spans = redactor.detectCandidates("a".repeat(4000) + "b", metrics);

        assertThat(spans).anyMatch(span -> "EMAIL_ADDRESS".equals(span.category()));
        assertThat(metrics.failures).containsExactly("deep");
    }

    private static SensitiveDataDetector throwing(String name, Supplier<? extends Error> errorSupplier) {
        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                throw errorSupplier.get();
            }

            @Override
            public List<SensitiveSpan> detect(String text, MatchDeadline deadline) {
                throw errorSupplier.get();
            }
        };
    }

    private static SensitiveDataDetector runaway(String name, String regex) {
        Pattern pattern = Pattern.compile(regex);

        return new SensitiveDataDetector() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public List<SensitiveSpan> detect(String text) {
                return detect(text, MatchDeadline.unbounded());
            }

            @Override
            public List<SensitiveSpan> detect(String text, MatchDeadline deadline) {
                pattern.matcher(deadline.bound(text))
                    .find();

                return List.of();
            }
        };
    }

    @Test
    void testExtraCandidatesAreRedactedAlongsideTheDetectedOnes() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        SensitiveDataRedactor.RedactionResult result = redactor.redactWithSpans(
            "abc def", BOTH, 0.0, null, List.of(SensitiveSpan.of(SensitiveKind.PII, "ACME_ID", 4, 7)));

        assertThat(result.text()).isEqualTo("[REDACTED_EMAIL] [REDACTED_ACME_ID]");
    }

    @Test
    void testAnExtraCandidateOverlappingADetectedOneResolvesByTheExistingRule() {
        SensitiveDataRedactor piiDetected = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)));

        assertThat(
            piiDetected.redactWithSpans(
                "call 4111111111111111 now", BOTH, 0.0, null,
                List.of(SensitiveSpan.of(SensitiveKind.SECRET, "ACME_KEY", 5, 21)))
                .text())
                    .as("SECRET beats an overlapping PII, whichever list it came from")
                    .isEqualTo("call [REDACTED_ACME_KEY] now");

        SensitiveDataRedactor secretDetected = redactor(
            fixed("secret", true, SensitiveSpan.of(SensitiveKind.SECRET, "ACME_KEY", 5, 21)));

        assertThat(
            secretDetected.redactWithSpans(
                "call 4111111111111111 now", BOTH, 0.0, null,
                List.of(SensitiveSpan.of(SensitiveKind.PII, "CC", 5, 21)))
                .text())
                    .as("and the same pair resolves the same way with the roles reversed")
                    .isEqualTo("call [REDACTED_ACME_KEY] now");
    }

    @Test
    void testAnExtraCandidateBelowTheThresholdIsDroppedLikeAnyOther() {
        SensitiveDataRedactor redactor = redactor(fixed("pii", true));

        assertThat(
            redactor.redactWithSpans(
                "abc def", BOTH, 0.5, null,
                List.of(new SensitiveSpan(SensitiveKind.PII, "ACME_WEAK", 0, 3, 0.2)))
                .text())
                    .isEqualTo("abc def");
    }

    @Test
    void testNoExtraCandidatesBehavesExactlyAsTheOverloadWithout() {
        SensitiveDataRedactor redactor = redactor(
            fixed("pii", true, SensitiveSpan.of(SensitiveKind.PII, "EMAIL", 0, 3)));

        assertThat(redactor.redactWithSpans("abc def", BOTH, 0.0, null, List.of()))
            .isEqualTo(redactor.redactWithSpans("abc def", BOTH, 0.0, null));
    }
}
