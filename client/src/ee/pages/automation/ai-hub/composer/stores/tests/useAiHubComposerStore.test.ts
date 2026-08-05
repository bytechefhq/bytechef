import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubComposerStore} from '../useAiHubComposerStore';

describe('useAiHubComposerStore', () => {
    beforeEach(() => {
        aiHubComposerStore.setState({referencedResources: []});
    });

    describe('addReference', () => {
        it('adds a new resource to referencedResources', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]).toEqual({id: 'file-1', kind: 'file', name: 'readme.md'});
        });

        it('adds multiple resources of different kinds', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
            aiHubComposerStore.getState().addReference({id: 'dt-1', kind: 'dataTable', name: 'Contacts'});
            aiHubComposerStore.getState().addReference({id: 'kb-1', kind: 'knowledgeBase', name: 'Docs'});

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(4);
        });

        it('does not add a duplicate resource with the same id and kind', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
        });

        it('allows resources with the same id but different kinds', () => {
            aiHubComposerStore.getState().addReference({id: 'shared-id', kind: 'file', name: 'File A'});
            aiHubComposerStore.getState().addReference({id: 'shared-id', kind: 'workflow', name: 'Flow A'});

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(2);
        });
    });

    describe('removeReference', () => {
        it('removes the matching resource by id and kind', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

            aiHubComposerStore.getState().removeReference('file-1', 'file');

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]!.id).toBe('wf-1');
        });

        it('does not remove resources that only match the id but not the kind', () => {
            aiHubComposerStore.getState().addReference({id: 'shared-id', kind: 'file', name: 'File A'});
            aiHubComposerStore.getState().addReference({id: 'shared-id', kind: 'workflow', name: 'Flow A'});

            aiHubComposerStore.getState().removeReference('shared-id', 'file');

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
            expect(referencedResources[0]!.kind).toBe('workflow');
        });

        it('is a no-op when the resource does not exist', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});

            aiHubComposerStore.getState().removeReference('nonexistent', 'file');

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(1);
        });
    });

    describe('clear', () => {
        it('removes all referenced resources', () => {
            aiHubComposerStore.getState().addReference({id: 'file-1', kind: 'file', name: 'readme.md'});
            aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

            aiHubComposerStore.getState().clear();

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(0);
        });

        it('is a no-op when store is already empty', () => {
            aiHubComposerStore.getState().clear();

            const {referencedResources} = aiHubComposerStore.getState();

            expect(referencedResources).toHaveLength(0);
        });
    });
});
