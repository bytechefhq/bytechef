import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubComposerStore} from '../useAiHubComposerStore';

describe('useAiHubComposerStore', () => {
    beforeEach(() => {
        aiHubComposerStore.setState({referencedResources: [], resourcePickerOpen: false});
    });

    describe('addReference', () => {
        it('adds a new resource to referencedResources', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]).toEqual({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
        });

        it('adds multiple resources', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
            aiHubComposerStore.getState().addReference({id: 'wf-2', kind: 'workflow', name: 'Other Flow'});

            expect(aiHubComposerStore.getState().referencedResources).toHaveLength(2);
        });

        it('does not add a duplicate resource with the same id and kind', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

            expect(aiHubComposerStore.getState().referencedResources).toHaveLength(1);
        });

        it('keeps the tab link and ownership on the reference', () => {
            aiHubComposerStore
                .getState()
                .addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow', ownsTab: true, tabId: 'tab-1'});

            expect(aiHubComposerStore.getState().referencedResources[0]).toMatchObject({
                ownsTab: true,
                tabId: 'tab-1',
            });
        });
    });

    describe('removeReference', () => {
        it('removes the matching resource by id and kind', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
            aiHubComposerStore.getState().addReference({id: 'wf-2', kind: 'workflow', name: 'Other Flow'});

            aiHubComposerStore.getState().removeReference('wf-1', 'workflow');

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]!.id).toBe('wf-2');
        });

        it('is a no-op when the resource does not exist', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

            aiHubComposerStore.getState().removeReference('nonexistent', 'workflow');

            expect(aiHubComposerStore.getState().referencedResources).toHaveLength(1);
        });
    });

    describe('clear', () => {
        it('removes all referenced resources and closes the picker', () => {
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
            aiHubComposerStore.getState().setResourcePickerOpen(true);

            aiHubComposerStore.getState().clear();

            expect(aiHubComposerStore.getState().referencedResources).toHaveLength(0);
            expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(false);
        });

        it('is a no-op when store is already empty', () => {
            aiHubComposerStore.getState().clear();

            expect(aiHubComposerStore.getState().referencedResources).toHaveLength(0);
        });
    });

    describe('setResourcePickerOpen', () => {
        it('toggles the picker open flag', () => {
            aiHubComposerStore.getState().setResourcePickerOpen(true);

            expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(true);

            aiHubComposerStore.getState().setResourcePickerOpen(false);

            expect(aiHubComposerStore.getState().resourcePickerOpen).toBe(false);
        });
    });
});
