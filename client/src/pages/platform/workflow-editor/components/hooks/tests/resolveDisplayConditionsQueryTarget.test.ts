import {resolveDisplayConditionsQueryTarget} from '@/pages/platform/workflow-editor/components/hooks/resolveDisplayConditionsQueryTarget';
import {describe, expect, it} from 'vitest';

describe('resolveDisplayConditionsQueryTarget', () => {
    it('fetches the regular endpoint for a plain workflow node on the properties tab', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: 'logger_1',
            })
        ).toBe('regular');
    });

    it('fetches the cluster endpoint for a cluster element on the properties tab', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: 'openAi_1',
                currentNodeClusterElementType: 'model',
                currentNodeName: 'openAi_1',
            })
        ).toBe('cluster');
    });

    it('fetches nothing while another tab is active', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'description',
                awaitingFirstSave: false,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: 'logger_1',
            })
        ).toBe('none');
    });

    it('fetches nothing while no node is focused', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: undefined,
            })
        ).toBe('none');
    });

    it('never fetches for the manual trigger', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: 'manual',
            })
        ).toBe('none');
    });

    it('fetches nothing for a freshly added node whose first save is still pending', () => {
        // The panel opens optimistically before the server persists the node. Fetching display
        // conditions for it 400s with "Workflow node with name: logger_4 does not exist" until the
        // save lands, and the failed query never recovers once it does.
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: true,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: 'logger_4',
            })
        ).toBe('none');
    });

    it('fetches nothing for a freshly added cluster element whose first save is still pending', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: true,
                currentClusterElementName: 'openAi_1',
                currentNodeClusterElementType: 'model',
                currentNodeName: 'openAi_1',
            })
        ).toBe('none');
    });

    it('fetches a persisted node even while a different node awaits its first save', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: undefined,
                currentNodeClusterElementType: undefined,
                currentNodeName: 'logger_1',
            })
        ).toBe('regular');
    });

    it('fetches nothing during the close race where clusterElementType cleared but the name still lags', () => {
        expect(
            resolveDisplayConditionsQueryTarget({
                activeTab: 'properties',
                awaitingFirstSave: false,
                currentClusterElementName: 'openAi_1',
                currentNodeClusterElementType: undefined,
                currentNodeName: 'openAi_1',
            })
        ).toBe('none');
    });
});
