/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder.CatalogEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pin the catalog-hash contract that gates the cold-start skip in {@link ToolSearchCatalogFeeder#populate()}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ToolSearchCatalogFeederTest {

    @Test
    void testHashIsStableAcrossInputReordering() {
        // The skip check compares a freshly computed hash to a hash stored on a prior boot. If the input order
        // shifts between boots (different ClusterElementDefinitionService iteration order under JVM upgrades, etc.)
        // the skip MUST still fire when the content matches. Sorting inside computeCatalogHash protects against
        // that — pin it here.
        List<CatalogEntry> order1 = List.of(
            new CatalogEntry("slack_send", "send a slack message"),
            new CatalogEntry("github_listPrs", "list pull requests"),
            new CatalogEntry("linear_createIssue", "create a linear issue"));

        List<CatalogEntry> order2 = List.of(
            new CatalogEntry("linear_createIssue", "create a linear issue"),
            new CatalogEntry("slack_send", "send a slack message"),
            new CatalogEntry("github_listPrs", "list pull requests"));

        assertThat(ToolSearchCatalogFeeder.computeCatalogHash(order1))
            .isEqualTo(ToolSearchCatalogFeeder.computeCatalogHash(order2));
    }

    @Test
    void testHashDiffersWhenAnySummaryChanges() {
        // Description edit on a single component must invalidate the cached hash — otherwise we'd skip a boot
        // when the catalog actually changed and search quality silently degrades against the old embedding.
        List<CatalogEntry> base = List.of(
            new CatalogEntry("slack_send", "send a slack message"),
            new CatalogEntry("github_listPrs", "list pull requests"));

        List<CatalogEntry> edited = List.of(
            new CatalogEntry("slack_send", "send a slack message via webhook"),
            new CatalogEntry("github_listPrs", "list pull requests"));

        assertThat(ToolSearchCatalogFeeder.computeCatalogHash(base))
            .isNotEqualTo(ToolSearchCatalogFeeder.computeCatalogHash(edited));
    }

    @Test
    void testHashDiffersWhenToolIsAddedOrRemoved() {
        List<CatalogEntry> small = List.of(
            new CatalogEntry("slack_send", "send a slack message"));

        List<CatalogEntry> larger = List.of(
            new CatalogEntry("slack_send", "send a slack message"),
            new CatalogEntry("github_listPrs", "list pull requests"));

        assertThat(ToolSearchCatalogFeeder.computeCatalogHash(small))
            .isNotEqualTo(ToolSearchCatalogFeeder.computeCatalogHash(larger));
    }

    @Test
    void testEmptyCatalogHasStableHash() {
        // First-ever boot or a deployment that ships no tool-typed cluster elements still needs a deterministic
        // value so the readStoredHash / writeStoredHash round-trip works. The exact value isn't load-bearing, but
        // it MUST be the same across calls so the empty-catalog skip path doesn't thrash repopulate every boot.
        assertThat(ToolSearchCatalogFeeder.computeCatalogHash(List.of()))
            .isEqualTo(ToolSearchCatalogFeeder.computeCatalogHash(List.of()));
    }

    @Test
    void testHashCollisionsAreVanishinglyUnlikelyForPathologicalInputs() {
        // Defensive regression: a naive concatenation without separators would hash ["ab", "c"] the same as
        // ["a", "bc"]. The NUL separator between fields and between entries makes those distinct. This isn't a
        // real risk for tool catalogs (tool names don't bleed into summaries) but the test pins the boundary so a
        // future hash-recipe edit doesn't silently regress.
        List<CatalogEntry> a = List.of(new CatalogEntry("ab", "c"));
        List<CatalogEntry> b = List.of(new CatalogEntry("a", "bc"));

        assertThat(ToolSearchCatalogFeeder.computeCatalogHash(a))
            .isNotEqualTo(ToolSearchCatalogFeeder.computeCatalogHash(b));
    }
}
