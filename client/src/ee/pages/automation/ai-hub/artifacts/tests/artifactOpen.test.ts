import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {beforeEach, describe, expect, it} from 'vitest';

import {AiHubChatArtifactI} from '../../chats/api/chats.api';
import {handleArtifactQuickOpen, isArtifactClickable, isArtifactRemovable} from '../artifactOpen';

function buildArtifact(overrides: Partial<AiHubChatArtifactI> = {}): AiHubChatArtifactI {
    return {
        artifactId: 'wf-1',
        artifactName: 'Lead Scoring',
        chatId: 1,
        createdAt: new Date().toISOString(),
        id: 1,
        kind: 'WORKFLOW_CREATED',
        metadataJson: JSON.stringify({projectId: '5', projectWorkflowId: 12}),
        status: 'APPLIED',
        ...overrides,
    };
}

describe('artifactOpen', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            chatsSidebarCollapsed: false,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    it.each(['WORKFLOW_CREATED', 'WORKFLOW_UPDATED', 'WORKFLOW_REFERENCED'] as const)(
        'opens a workflow tab for a %s artifact from its metadata',
        (kind) => {
            handleArtifactQuickOpen(buildArtifact({kind}));

            const state = aiHubTabsStore.getState();

            expect(state.openTabs).toHaveLength(1);
            expect(state.openTabs[0]).toMatchObject({
                kind: 'workflow',
                name: 'Lead Scoring',
                projectId: '5',
                projectWorkflowId: 12,
                workflowId: 'wf-1',
            });
            expect(state.rightPanelOpen).toBe(true);
        }
    );

    it('does nothing for a workflow artifact without a projectId', () => {
        handleArtifactQuickOpen(buildArtifact({metadataJson: null}));

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('does nothing for a non-workflow kind', () => {
        handleArtifactQuickOpen(buildArtifact({kind: 'FILE_CREATED'}));

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('treats unparseable metadata as missing', () => {
        expect(isArtifactClickable(buildArtifact({metadataJson: '{not json'}))).toBe(false);
    });

    it('is clickable only for a workflow kind that carries a projectId', () => {
        expect(isArtifactClickable(buildArtifact())).toBe(true);
        expect(isArtifactClickable(buildArtifact({kind: 'WORKFLOW_REFERENCED'}))).toBe(true);
        expect(isArtifactClickable(buildArtifact({metadataJson: null}))).toBe(false);
        expect(isArtifactClickable(buildArtifact({kind: 'FILE_CREATED'}))).toBe(false);
    });

    it('is removable only for WORKFLOW_REFERENCED', () => {
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_REFERENCED'}))).toBe(true);
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_CREATED'}))).toBe(false);
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_UPDATED'}))).toBe(false);
    });
});
