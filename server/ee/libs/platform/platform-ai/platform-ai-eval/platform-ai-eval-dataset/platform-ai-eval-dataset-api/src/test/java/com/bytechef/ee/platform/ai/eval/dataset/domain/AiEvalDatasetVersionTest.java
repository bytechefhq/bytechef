/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetVersionTest {

    @Test
    void testConstructorRejectsNullDatasetId() {
        assertThrows(NullPointerException.class, () -> new AiEvalDatasetVersion(null, 1));
    }

    @Test
    void testConstructorRejectsNonPositiveVersionNumber() {
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDatasetVersion(1L, 0));
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDatasetVersion(1L, -1));
    }

    @Test
    void testNewVersionStartsUnfrozen() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        assertFalse(version.isFrozen());
    }

    @Test
    void testFreezeIsIdempotent() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        version.freeze();
        version.freeze();

        assertTrue(version.isFrozen());
    }

    /**
     * The {@code frozen} flag is one-way by design: a frozen version is an immutable snapshot pinned by experiments and
     * downstream consumers. Once {@link AiEvalDatasetVersion#freeze()} flips the flag, no public API may unset it — a
     * future caller that tries via {@code setFrozen(false)} would silently break the comparison contract for every
     * consumer that already pinned the snapshot. Pinning the package-private modifier here turns the regression into a
     * compile-time-visible violation.
     */
    @Test
    void testSetFrozenIsPackagePrivate() throws NoSuchMethodException {
        Method setFrozen = AiEvalDatasetVersion.class.getDeclaredMethod("setFrozen", boolean.class);

        int modifiers = setFrozen.getModifiers();

        assertThat(Modifier.isPublic(modifiers))
            .as("setFrozen must NOT be public — re-publicizing breaks the one-way freeze invariant")
            .isFalse();
        assertThat(Modifier.isProtected(modifiers))
            .as("setFrozen must NOT be protected")
            .isFalse();
        assertThat(Modifier.isPrivate(modifiers))
            .as("setFrozen must be package-private (not private), so Spring Data JDBC hydration works")
            .isFalse();
    }

    /**
     * {@link AiEvalDatasetVersion#freeze()} must be public so service-layer callers can flip the snapshot to immutable.
     * A regression that demoted it to package-private would force every promoter into the same package, which defeats
     * the purpose of the public freeze API.
     */
    @Test
    void testFreezeIsPublic() throws NoSuchMethodException {
        Method freeze = AiEvalDatasetVersion.class.getDeclaredMethod("freeze");

        assertTrue(
            Modifier.isPublic(freeze.getModifiers()),
            "freeze() must be public — promoters live outside this package");
    }

    @Test
    void testPublicSettersRejectMutationOnFrozenVersion() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        version.freeze();

        IllegalStateException labelError = assertThrows(
            IllegalStateException.class, () -> version.setLabel("renamed"));
        IllegalStateException versionNumberError = assertThrows(
            IllegalStateException.class, () -> version.setVersionNumber(99));
        IllegalStateException datasetIdError = assertThrows(
            IllegalStateException.class, () -> version.setDatasetId(2L));

        assertTrue(labelError.getMessage()
            .contains("label"));
        assertTrue(versionNumberError.getMessage()
            .contains("versionNumber"));
        assertTrue(datasetIdError.getMessage()
            .contains("datasetId"));
    }

    @Test
    void testSettersAcceptMutationOnUnfrozenVersion() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        version.setLabel("renamed");
        version.setVersionNumber(2);
        version.setDatasetId(3L);

        assertThat(version.getLabel()).isEqualTo("renamed");
        assertThat(version.getVersionNumber()).isEqualTo(2);
        assertThat(version.getDatasetId()).isEqualTo(3L);
    }

    @Test
    void testSetVersionNumberRejectsNonPositive() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        assertThrows(IllegalArgumentException.class, () -> version.setVersionNumber(0));
        assertThrows(IllegalArgumentException.class, () -> version.setVersionNumber(-5));
    }

    @Test
    void testSetDatasetIdRejectsNull() {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(1L, 1);

        assertThrows(NullPointerException.class, () -> version.setDatasetId(null));
    }
}
