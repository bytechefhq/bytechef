import {describe, expect, it} from 'vitest';

import isDynamicPropertiesQueryEnabled from './isDynamicPropertiesQueryEnabled';

/**
 * Tests for the DYNAMIC_PROPERTIES query `enabled` gate used in Property.tsx.
 *
 * Regression for the "Response to Workflow Call" Output Schema bug: a freshly added connectionless
 * node has no `connections` field (it is `undefined`), while a node loaded from the server has
 * `connections: []`. The old gate checked `connections.length === 0`, which is `false` for
 * `undefined`, so schema-derived properties (e.g. the "Message" field) only appeared after a page
 * reload. The gate must treat a missing `connections` field the same as an empty one.
 */
describe('isDynamicPropertiesQueryEnabled', () => {
    it('enables the query for a freshly added connectionless node (connections undefined)', () => {
        expect(isDynamicPropertiesQueryEnabled({clusterElementContext: false, connections: undefined})).toBe(true);
    });

    it('enables the query for a server-loaded connectionless node (connections [])', () => {
        expect(isDynamicPropertiesQueryEnabled({clusterElementContext: false, connections: []})).toBe(true);
    });

    it('enables the query when a cluster element context is present', () => {
        expect(isDynamicPropertiesQueryEnabled({clusterElementContext: true, connections: [{required: true}]})).toBe(
            true
        );
    });

    it('enables the query when the node has a configured connection', () => {
        expect(
            isDynamicPropertiesQueryEnabled({
                clusterElementContext: false,
                connectionId: 42,
                connections: [{required: true}],
            })
        ).toBe(true);
    });

    it('disables the query when a connection is required but not yet configured', () => {
        expect(isDynamicPropertiesQueryEnabled({clusterElementContext: false, connections: [{required: true}]})).toBe(
            false
        );
    });

    it('disables the query when a connection is required but only a falsy connectionId is set', () => {
        expect(
            isDynamicPropertiesQueryEnabled({
                clusterElementContext: false,
                connectionId: 0,
                connections: [{required: true}],
            })
        ).toBe(false);
    });
});
