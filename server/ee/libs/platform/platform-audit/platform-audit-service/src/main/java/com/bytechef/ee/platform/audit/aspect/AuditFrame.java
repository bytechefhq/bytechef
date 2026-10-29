/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import com.bytechef.platform.audit.Audited;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

/**
 * State of one audited invocation, shared between {@link AuditAspect} and {@link AuditCaptureAspect}, kept on a
 * thread-local stack. {@link AuditAspect} pushes one frame per advised invocation and pops it on exit;
 * {@link AuditCaptureAspect}, which runs inside it for the same invocation, reads the top.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditFrame {

    private static final ThreadLocal<Deque<AuditFrame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private final @Nullable Audited audited;
    private @Nullable Object captured;
    private @Nullable String captureError;

    private AuditFrame(@Nullable Audited audited) {
        this.audited = audited;
    }

    static AuditFrame push(@Nullable Audited audited) {
        AuditFrame auditFrame = new AuditFrame(audited);

        FRAMES.get()
            .push(auditFrame);

        return auditFrame;
    }

    static @Nullable AuditFrame current() {
        return FRAMES.get()
            .peek();
    }

    static void pop() {
        Deque<AuditFrame> auditFrames = FRAMES.get();

        auditFrames.pop();

        if (auditFrames.isEmpty()) {
            FRAMES.remove();
        }
    }

    @Nullable
    Audited getAudited() {
        return audited;
    }

    @Nullable
    Object getCaptured() {
        return captured;
    }

    void setCaptured(@Nullable Object captured) {
        this.captured = captured;
    }

    @Nullable
    String getCaptureError() {
        return captureError;
    }

    void setCaptureError(@Nullable String captureError) {
        this.captureError = captureError;
    }
}
