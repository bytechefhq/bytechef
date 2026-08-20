/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.agent.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.agent.facade.AiAgentSharingFacade;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins that this controller reaches its rows through the gated agent facade and through nothing else.
 *
 * <p>
 * The controller carries no {@code @PreAuthorize} of its own and is not meant to — that convention has one failure
 * mode, which is a root query assembled here out of something lower down. The tempting shortcut is real and specific:
 * this surface's whole job is project sharing, {@code ProjectSharingFacade} is one import away, and calling it directly
 * would produce identical results while skipping {@code AGENT_EDIT} and the visibility precondition the agent-keyed
 * gate carries. Asserting the agent facade was called does not rule that out on its own; asserting the controller holds
 * NOTHING else is what makes the shortcut fail here rather than pass.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiAgentSharingGraphQlControllerAuthorizationTest {

    private static final long AGENT_ID = 3L;
    private static final long USER_ID = 7L;

    private final AiAgentSharingFacade aiAgentSharingFacade = mock(AiAgentSharingFacade.class);

    private final AiAgentSharingGraphQlController aiAgentSharingGraphQlController =
        new AiAgentSharingGraphQlController(aiAgentSharingFacade);

    @Test
    void testEveryOperationGoesThroughTheGatedFacade() {
        when(aiAgentSharingFacade.getAgentGrants(AGENT_ID)).thenReturn(List.of(USER_ID));

        assertThat(aiAgentSharingGraphQlController.aiAgentGrants(AGENT_ID)).containsExactly(USER_ID);
        assertThat(aiAgentSharingGraphQlController.setAiAgentVisibility(AGENT_ID, ResourceVisibility.PRIVATE)).isTrue();
        assertThat(aiAgentSharingGraphQlController.grantAiAgentAccess(AGENT_ID, USER_ID)).isTrue();
        assertThat(aiAgentSharingGraphQlController.revokeAiAgentAccess(AGENT_ID, USER_ID)).isTrue();

        verify(aiAgentSharingFacade).getAgentGrants(AGENT_ID);
        verify(aiAgentSharingFacade).setAgentVisibility(AGENT_ID, ResourceVisibility.PRIVATE);
        verify(aiAgentSharingFacade).grantAgentAccess(AGENT_ID, USER_ID);
        verify(aiAgentSharingFacade).revokeAgentAccess(AGENT_ID, USER_ID);
    }

    @Test
    void testHoldsNothingButTheGatedFacade() {
        List<Class<?>> fieldTypes = Arrays.stream(AiAgentSharingGraphQlController.class.getDeclaredFields())
            .filter(field -> !field.isSynthetic())
            .filter(field -> !Modifier.isStatic(field.getModifiers()))
            .map(Field::getType)
            .toList();

        assertThat(fieldTypes).containsExactly(AiAgentSharingFacade.class);
    }

    /**
     * {@code #agentId} is only a usable gate key on the facade while the parameter cannot be null: a boxed {@code null}
     * reaches {@code AutomationPermissionEvaluator} as a null target id. The schema says {@code ID!}, but that is a
     * second file's promise, and widening these would fail as an unboxing NPE at runtime rather than here.
     */
    @Test
    void testEveryOperationKeysOnAPrimitiveAgentId() {
        assertThat(Arrays.stream(AiAgentSharingGraphQlController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .map(Method::getParameterTypes)
            .map(parameterTypes -> parameterTypes[0]))
                .isNotEmpty()
                .allMatch(long.class::equals);
    }
}
