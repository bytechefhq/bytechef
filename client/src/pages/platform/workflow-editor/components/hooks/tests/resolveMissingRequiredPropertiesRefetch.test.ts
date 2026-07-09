import {resolveMissingRequiredPropertiesRefetch} from '@/pages/platform/workflow-editor/components/hooks/resolveMissingRequiredPropertiesRefetch';
import {describe, expect, it} from 'vitest';

describe('resolveMissingRequiredPropertiesRefetch', () => {
    it('refetches the regular endpoint for a plain workflow node', () => {
        // Regular node: name set, no cluster element name, no clusterElementType.
        expect(resolveMissingRequiredPropertiesRefetch('activeCampaign_1', undefined, undefined, undefined)).toBe(
            'regular'
        );
    });

    it('refetches the cluster endpoint for a cluster element', () => {
        // Cluster element: name === clusterElementName AND a clusterElementType is present.
        expect(
            resolveMissingRequiredPropertiesRefetch('activeCampaign_1', 'activeCampaign_1', 'tools', undefined)
        ).toBe('cluster');
    });

    it('refetches nothing while no node is focused', () => {
        expect(resolveMissingRequiredPropertiesRefetch(undefined, undefined, undefined, undefined)).toBe('none');
    });

    it('never refetches for the manual trigger', () => {
        expect(resolveMissingRequiredPropertiesRefetch('manual', undefined, undefined, undefined)).toBe('none');
    });

    it('refetches nothing for a freshly added node whose first save is still pending', () => {
        // The panel opens optimistically before the server persists the node; refetching it would 404
        // with "Workflow node with name: accelo_1 does not exist" until the save lands.
        expect(resolveMissingRequiredPropertiesRefetch('accelo_1', undefined, undefined, 'accelo_1')).toBe('none');
    });

    it('refetches a persisted node even while a different node awaits its first save', () => {
        expect(resolveMissingRequiredPropertiesRefetch('activeCampaign_1', undefined, undefined, 'accelo_1')).toBe(
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
            resolveMissingRequiredPropertiesRefetch('activeCampaign_1', 'activeCampaign_1', undefined, undefined)
        ).toBe('none');
    });

    it('refetches nothing in the inverse transient (type present but names disagree)', () => {
        // Symmetric transient: clusterElementType present but the names haven't converged yet. Neither the
        // cluster guard (needs name === clusterElementName) nor the regular guard (needs no type) matches.
        expect(resolveMissingRequiredPropertiesRefetch('activeCampaign_1', undefined, 'tools', undefined)).toBe('none');
    });
});
