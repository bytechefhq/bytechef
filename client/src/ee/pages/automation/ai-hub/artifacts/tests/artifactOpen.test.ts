import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {AiHubChatArtifactI} from '../../chats/api/chats.api';
import {handleArtifactQuickOpen} from '../artifactOpen';

const {mockGetProject, mockToastError} = vi.hoisted(() => ({
    mockGetProject: vi.fn(),
    mockToastError: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/automation/configuration');

    class MockProjectApi {
        getProject = mockGetProject;
    }

    return {
        ...actual,
        ProjectApi: MockProjectApi,
    };
});

vi.mock('sonner', () => ({
    toast: {
        error: mockToastError,
    },
}));

describe('handleArtifactQuickOpen', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            chatsSidebarCollapsed: true,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('opens a workflowExecution tab for a WORKFLOW_EXECUTION_STARTED artifact and does not call window.open', () => {
        const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

        const workflowExecutionArtifact: AiHubChatArtifactI = {
            artifactId: '777',
            artifactName: 'Run #777',
            chatId: 42,
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'WORKFLOW_EXECUTION_STARTED',
            metadataJson: null,
            status: 'APPLIED',
        };

        handleArtifactQuickOpen(workflowExecutionArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('workflowExecution');

        if (openedTab.kind === 'workflowExecution') {
            expect(openedTab.workflowExecutionId).toBe(777);
            expect(openedTab.name).toBe('Run #777');
        }

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(true);

        expect(openSpy).not.toHaveBeenCalled();
    });

    it('opens a skill tab for a SKILL_REFERENCED artifact using artifactId as the skillId', () => {
        const skillArtifact: AiHubChatArtifactI = {
            artifactId: '7',
            artifactName: 'Triage',
            chatId: 42,
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'SKILL_REFERENCED',
            metadataJson: null,
            status: 'APPLIED',
        };

        handleArtifactQuickOpen(skillArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('skill');

        if (openedTab.kind === 'skill') {
            expect(openedTab.skillId).toBe('7');
            expect(openedTab.name).toBe('Triage');
        }
    });

    it('opens a custom component tab for a CUSTOM_COMPONENT_REFERENCED artifact using artifactId as the customComponentId', () => {
        const customComponentArtifact: AiHubChatArtifactI = {
            artifactId: '9',
            artifactName: 'My Component',
            chatId: 42,
            createdAt: new Date().toISOString(),
            id: 1,
            kind: 'CUSTOM_COMPONENT_REFERENCED',
            metadataJson: null,
            status: 'APPLIED',
        };

        handleArtifactQuickOpen(customComponentArtifact);

        const openTabs = aiHubTabsStore.getState().openTabs;

        expect(openTabs).toHaveLength(1);

        const openedTab = openTabs[0]!;

        expect(openedTab.kind).toBe('customComponent');

        if (openedTab.kind === 'customComponent') {
            expect(openedTab.customComponentId).toBe('9');
            expect(openedTab.name).toBe('My Component');
        }
    });

    describe('CODE_WORKFLOW_REFERENCED', () => {
        function buildCodeWorkflowArtifact(overrides: Partial<AiHubChatArtifactI> = {}): AiHubChatArtifactI {
            return {
                artifactId: '11',
                artifactName: 'My Code Workflow',
                chatId: 42,
                createdAt: new Date().toISOString(),
                id: 1,
                kind: 'CODE_WORKFLOW_REFERENCED',
                metadataJson: null,
                status: 'APPLIED',
                ...overrides,
            };
        }

        beforeEach(() => {
            mockGetProject.mockReset();
            mockToastError.mockReset();
        });

        it('fetches the project and opens a codeWorkflow tab using its codeWorkflowLanguage, using artifactId as the projectId', async () => {
            mockGetProject.mockResolvedValue({codeWorkflowLanguage: 'PYTHON', id: 11});

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(mockGetProject).toHaveBeenCalledWith({id: 11});

            const openTabs = aiHubTabsStore.getState().openTabs;

            expect(openTabs).toHaveLength(1);

            const openedTab = openTabs[0]!;

            expect(openedTab.kind).toBe('codeWorkflow');

            if (openedTab.kind === 'codeWorkflow') {
                expect(openedTab.projectId).toBe('11');
                expect(openedTab.language).toBe('PYTHON');
                expect(openedTab.name).toBe('My Code Workflow');
            }

            expect(mockToastError).not.toHaveBeenCalled();
        });

        it('surfaces a toast and does not open a tab when the project has no codeWorkflowLanguage', async () => {
            mockGetProject.mockResolvedValue({id: 11});

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(mockToastError).toHaveBeenCalledWith(expect.stringContaining('is no longer a code workflow'));
        });

        it('surfaces a toast and does not open a tab when the project fetch fails', async () => {
            mockGetProject.mockRejectedValue(new Error('network down'));

            await handleArtifactQuickOpen(buildCodeWorkflowArtifact());

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(mockToastError).toHaveBeenCalledWith(expect.stringContaining('network down'));
        });
    });
});
