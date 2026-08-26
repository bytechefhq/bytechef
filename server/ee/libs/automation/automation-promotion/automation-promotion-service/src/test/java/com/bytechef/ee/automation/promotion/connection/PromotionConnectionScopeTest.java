/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.promotion.deployment.ProjectDeploymentPromoter.SourceBinding;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionConnectionScopeTest {

    @Test
    void testMappingForAConnectionTheSourceUsesIsAccepted() {
        assertThatCode(
            () -> PromotionConnectionScope.checkMappedConnectionsBelongToSource(Set.of(11L, 12L), Map.of(11L, 22L)))
                .doesNotThrowAnyException();
    }

    @Test
    void testMappingForAConnectionTheSourceDoesNotUseIsRejected() {
        assertThatThrownBy(
            () -> PromotionConnectionScope.checkMappedConnectionsBelongToSource(Set.of(11L), Map.of(999L, 22L)))
                .isInstanceOf(ConfigurationException.class)
                .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
                .isEqualTo(EnvironmentPromotionErrorType.TARGET_CONNECTION_INVALID.getErrorKey());
    }

    @Test
    void testOneForeignMappingAmongValidOnesIsRejected() {
        assertThatThrownBy(
            () -> PromotionConnectionScope.checkMappedConnectionsBelongToSource(
                Set.of(11L, 12L), Map.of(11L, 22L, 999L, 23L)))
                    .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void testNoMappingsAreAlwaysAccepted() {
        assertThatCode(() -> PromotionConnectionScope.checkMappedConnectionsBelongToSource(Set.of(), Map.of()))
            .doesNotThrowAnyException();
    }

    @Test
    void testSourceConnectionIdsCollectsEveryBoundConnection() {
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(
            List.of(sourceBinding(11L), sourceBinding(12L)));

        assertThat(sourceConnectionIds).containsExactly(11L, 12L);
    }

    /**
     * Two nodes of the same workflow routinely bind the same connection, so the result is a set, not a bag. The order
     * is nonetheless stable: a {@code LinkedHashSet} keeps the ids in the order the bindings name them, which is what
     * makes the promotion preview's connection list reproducible between runs.
     */
    @Test
    void testSourceConnectionIdsDeduplicatesAndKeepsEncounterOrder() {
        Set<Long> sourceConnectionIds = PromotionConnectionScope.sourceConnectionIds(
            List.of(sourceBinding(30L), sourceBinding(10L), sourceBinding(30L), sourceBinding(20L)));

        assertThat(sourceConnectionIds).containsExactly(30L, 10L, 20L);
    }

    @Test
    void testSourceConnectionIdsOfNoBindingsIsEmpty() {
        assertThat(PromotionConnectionScope.sourceConnectionIds(List.of())).isEmpty();
    }

    /**
     * The set is supplied by the handler, whatever it unions into it — an MCP server folds its
     * {@code mcp_component.connection_id}s in beside its deployment bindings, and this class must accept those on
     * exactly the same terms.
     */
    @Test
    void testTheSuppliedSetIsTheOnlyAuthorityOnWhatBelongsToTheSource() {
        assertThatCode(
            () -> PromotionConnectionScope.checkMappedConnectionsBelongToSource(Set.of(77L), Map.of(77L, 88L)))
                .doesNotThrowAnyException();
    }

    private static SourceBinding sourceBinding(long connectionId) {
        return new SourceBinding("uuid", "Workflow", "node", "key", connectionId);
    }
}
