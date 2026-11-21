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

    it('opens an AI Agent tab for an AI_AGENT_REFERENCED artifact', () => {
        handleArtifactQuickOpen(
            buildArtifact({
                artifactId: '42',
                artifactName: 'Support Agent',
                kind: 'AI_AGENT_REFERENCED',
                metadataJson: null,
            })
        );

        const state = aiHubTabsStore.getState();

        expect(state.openTabs).toHaveLength(1);
        expect(state.openTabs[0]).toMatchObject({aiAgentId: '42', kind: 'aiAgent', name: 'Support Agent'});
    });

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

    it('is clickable and removable for AI_AGENT_REFERENCED', () => {
        const aiAgentArtifact = buildArtifact({artifactId: '42', kind: 'AI_AGENT_REFERENCED', metadataJson: null});

        expect(isArtifactClickable(aiAgentArtifact)).toBe(true);
        expect(isArtifactRemovable(aiAgentArtifact)).toBe(true);
    });

    it('is removable for WORKFLOW_REFERENCED but not for the created and updated workflow kinds', () => {
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_REFERENCED'}))).toBe(true);
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_CREATED'}))).toBe(false);
        expect(isArtifactRemovable(buildArtifact({kind: 'WORKFLOW_UPDATED'}))).toBe(false);
    });
});
