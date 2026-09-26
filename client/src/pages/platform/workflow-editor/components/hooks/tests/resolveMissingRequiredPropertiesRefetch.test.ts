import {resolveMissingRequiredPropertiesRefetch} from '@/pages/platform/workflow-editor/components/hooks/resolveMissingRequiredPropertiesRefetch';
import {describe, expect, it} from 'vitest';

describe('resolveMissingRequiredPropertiesRefetch', () => {
    it('refetches the regular endpoint for a plain workflow node', () => {
        // Regular node: name set, no cluster element name, no clusterElementType.
        expect(resolveMissingRequiredPropertiesRefetch('1', 'activeCampaign_1', undefined, undefined, false)).toBe(
            'regular'
        );
    });

    it('refetches the cluster endpoint for a cluster element', () => {
        // Cluster element: name === clusterElementName AND a clusterElementType is present.
        expect(
            resolveMissingRequiredPropertiesRefetch('1', 'activeCampaign_1', 'activeCampaign_1', 'tools', false)
        ).toBe('cluster');
    });

    it('refetches nothing while no node is focused', () => {
        expect(resolveMissingRequiredPropertiesRefetch('1', undefined, undefined, undefined, false)).toBe('none');
    });

    it('never refetches for the manual trigger', () => {
        expect(resolveMissingRequiredPropertiesRefetch('1', 'manual', undefined, undefined, false)).toBe('none');
    });

    it('refetches nothing for a freshly added node whose first save is still pending', () => {
        // The panel opens optimistically before the server persists the node; refetching it would 404
        // with "Workflow node with name: accelo_1 does not exist" until the save lands.
        expect(resolveMissingRequiredPropertiesRefetch('1', 'accelo_1', undefined, undefined, true)).toBe('none');
    });

    it('refetches a persisted node even while a different node awaits its first save', () => {
        expect(resolveMissingRequiredPropertiesRefetch('1', 'activeCampaign_1', undefined, undefined, false)).toBe(
            'regular'
        );
    });

    it('refetches nothing during the close race where clusterElementType cleared but the name still lags', () => {
        // The bug: on cluster-element editor close, currentNode.clusterElementType clears in the store
        // (live) before currentNodeName updates (local state, lags by an effect). currentNodeName and
        // currentClusterElementName are set together, so both still hold the cluster element's name. Branching
        // on clusterElementType alone would fire the plain endpoint with "activeCampaign_1" → server 404.
        // The equality guard keeps this at 'none' until the local name state reconciles.
        expect(
            resolveMissingRequiredPropertiesRefetch('1', 'activeCampaign_1', 'activeCampaign_1', undefined, false)
        ).toBe('none');
    });

    it('refetches nothing in the inverse transient (type present but names disagree)', () => {
        // Symmetric transient: clusterElementType present but the names haven't converged yet. Neither the
        // cluster guard (needs name === clusterElementName) nor the regular guard (needs no type) matches.
        expect(resolveMissingRequiredPropertiesRefetch('1', 'activeCampaign_1', undefined, 'tools', false)).toBe(
            'none'
        );
    });

    it('refetches nothing while the workflow has no id yet', () => {
        // refetch() bypasses the queries' `enabled: !!workflow.id` guard, so without this check the query
        // goes out with workflowId null and the server rejects it: "Variable 'workflowId' has coerced Null
        // value for NonNull type 'String!'".
        expect(
            resolveMissingRequiredPropertiesRefetch(undefined, 'activeCampaign_1', undefined, undefined, false)
        ).toBe('none');
        expect(
            resolveMissingRequiredPropertiesRefetch(undefined, 'activeCampaign_1', 'activeCampaign_1', 'tools', false)
        ).toBe('none');
    });
});
