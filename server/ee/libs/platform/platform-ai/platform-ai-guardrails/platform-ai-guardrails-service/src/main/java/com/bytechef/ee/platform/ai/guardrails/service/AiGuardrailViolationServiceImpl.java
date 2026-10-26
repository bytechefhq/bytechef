/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.service;

import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailViolation;
import com.bytechef.ee.platform.ai.guardrails.repository.AiGuardrailViolationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class AiGuardrailViolationServiceImpl implements AiGuardrailViolationService {

    private static final int MAX_LIMIT = 1000;

    private final AiGuardrailViolationRepository aiGuardrailViolationRepository;

    @SuppressFBWarnings("EI")
    public AiGuardrailViolationServiceImpl(AiGuardrailViolationRepository aiGuardrailViolationRepository) {
        this.aiGuardrailViolationRepository = aiGuardrailViolationRepository;
    }

    @Override
    public List<AiGuardrailViolation> getViolations(@Nullable Long workspaceId, int limit) {
        List<AiGuardrailViolation> violations =
            aiGuardrailViolationRepository.findAllByWorkspaceIdOrderByCreatedDateDesc(workspaceId);

        int effectiveLimit = Math.clamp(limit, 1, MAX_LIMIT);

        if (violations.size() <= effectiveLimit) {
            return violations;
        }

        return List.copyOf(violations.subList(0, effectiveLimit));
    }
}
