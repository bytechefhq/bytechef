import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubTabsStore, getTabGenericId, useAiHubTabsStore} from '../useAiHubTabsStore';

const WORKFLOW_ONE = {name: 'My Workflow', projectId: 'proj-1', projectWorkflowId: 10, workflowId: 'wf-1'};
const WORKFLOW_TWO = {name: 'Other Workflow', projectId: 'proj-1', projectWorkflowId: 11, workflowId: 'wf-2'};
const WORKFLOW_THREE = {name: 'Third Workflow', projectId: 'proj-2', projectWorkflowId: 21, workflowId: 'wf-3'};

describe('useAiHubTabsStore', () => {
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

    it('opens a workflow tab with kind: workflow and correct fields', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openWorkflowTab(WORKFLOW_ONE);
        });

        expect(result.current.openTabs).toHaveLength(1);

        const tab = result.current.openTabs[0]!;

        expect(tab).toMatchObject({...WORKFLOW_ONE, id: tabId, kind: 'workflow'});
        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('focuses the existing tab when the same workflow is opened a second time', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';

        act(() => {
            firstTabId = result.current.openWorkflowTab(WORKFLOW_ONE);
            result.current.openWorkflowTab(WORKFLOW_THREE);
        });

        expect(result.current.activeTabId).not.toBe(firstTabId);

        act(() => {
            secondTabId = result.current.openWorkflowTab(WORKFLOW_ONE);
        });

        expect(secondTabId).toBe(firstTabId);
        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).toBe(firstTabId);
    });

    it('re-points the existing project-scoped workflow tab when another workflow of the same project opens', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openWorkflowTab(WORKFLOW_ONE);
            result.current.openWorkflowTab(WORKFLOW_TWO);
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(firstTabId);
        expect(result.current.openTabs[0]).toMatchObject({...WORKFLOW_TWO, id: firstTabId});
    });

    it('opens separate tabs for workflows from different projects', () => {
        aiHubTabsStore.getState().openWorkflowTab(WORKFLOW_ONE);
        aiHubTabsStore.getState().openWorkflowTab(WORKFLOW_THREE);

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(2);
    });

    it('removes a tab and picks the neighboring tab as active', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';
        let thirdTabId = '';

        act(() => {
            firstTabId = result.current.openWorkflowTab({...WORKFLOW_ONE, projectId: 'p-1'});
            secondTabId = result.current.openWorkflowTab({...WORKFLOW_ONE, projectId: 'p-2'});
            thirdTabId = result.current.openWorkflowTab({...WORKFLOW_ONE, projectId: 'p-3'});
            result.current.setActiveTab(secondTabId);
        });

        act(() => {
            result.current.closeTab(secondTabId);
        });

        expect(result.current.openTabs.map((tab) => tab.id)).toEqual([firstTabId, thirdTabId]);
        expect(result.current.activeTabId).toBe(thirdTabId);
    });

    it('clears activeTabId when the last tab is closed', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openWorkflowTab(WORKFLOW_ONE);
        });

        act(() => {
            result.current.closeTab(tabId);
        });

        expect(result.current.openTabs).toHaveLength(0);
        expect(result.current.activeTabId).toBeUndefined();
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('ignores setActiveTab for an unknown tab id', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openWorkflowTab(WORKFLOW_ONE);
            result.current.setActiveTab('missing');
        });

        expect(result.current.activeTabId).toBe(tabId);
    });

    it('returns the workflow id as the generic tab id', () => {
        const tabId = aiHubTabsStore.getState().openWorkflowTab(WORKFLOW_ONE);

        const tab = aiHubTabsStore.getState().openTabs.find((openTab) => openTab.id === tabId)!;

        expect(getTabGenericId(tab)).toBe('wf-1');
    });

    describe('setActiveChatId home to chat hand-off', () => {
        it('inherits the open tabs and rightPanelOpen from home view when first chat is created', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.openWorkflowTab(WORKFLOW_ONE);
            });

            expect(result.current.activeChatId).toBeUndefined();
            expect(result.current.rightPanelOpen).toBe(true);

            act(() => {
                result.current.setActiveChatId(1);
            });

            expect(result.current.activeChatId).toBe(1);
            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('keeps every tab opened on the home view, with the active tab unchanged', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            let firstTabId = '';

            act(() => {
                firstTabId = result.current.openWorkflowTab(WORKFLOW_ONE);

                result.current.openWorkflowTab(WORKFLOW_THREE);
                result.current.setActiveTab(firstTabId);
            });

            act(() => {
                result.current.setActiveChatId(1);
            });

            expect(result.current.openTabs.map((tab) => tab.workflowId)).toEqual(['wf-1', 'wf-3']);
            expect(result.current.activeTabId).toBe(firstTabId);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('carries a closed resource panel through the hand-off', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.openWorkflowTab(WORKFLOW_ONE);
                result.current.setRightPanelOpen(false);
            });

            act(() => {
                result.current.setActiveChatId(1);
            });

            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(false);
        });

        it('does not filter a restored snapshot on a chat to chat switch', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setActiveChatId(1);

                result.current.openWorkflowTab(WORKFLOW_ONE);
            });

            act(() => {
                result.current.setActiveChatId(2);
            });

            expect(result.current.openTabs).toHaveLength(0);

            act(() => {
                result.current.setActiveChatId(1);
            });

            expect(result.current.openTabs).toHaveLength(1);
        });

        it('restores the right panel per chat on switch (closed for chats with no snapshot)', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setActiveChatId(1);
                result.current.openWorkflowTab(WORKFLOW_ONE);
            });

            expect(result.current.rightPanelOpen).toBe(true);

            act(() => {
                result.current.setActiveChatId(2);
            });

            expect(result.current.openTabs).toHaveLength(0);
            expect(result.current.rightPanelOpen).toBe(false);

            act(() => {
                result.current.setActiveChatId(1);
            });

            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);
        });
    });

    describe('chatsSidebarCollapsed', () => {
        it('starts open (not collapsed), like the sidebar on every other page', () => {
            aiHubTabsStore.getState().reset();

            expect(aiHubTabsStore.getState().chatsSidebarCollapsed).toBe(false);
        });

        it('setChatsSidebarCollapsed toggles the hidden flag', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setChatsSidebarCollapsed(false);
            });

            expect(result.current.chatsSidebarCollapsed).toBe(false);

            act(() => {
                result.current.setChatsSidebarCollapsed(true);
            });

            expect(result.current.chatsSidebarCollapsed).toBe(true);
        });

        it('opening or closing the resource panel leaves the sidebar state alone', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setChatsSidebarCollapsed(false);
                result.current.setRightPanelOpen(true);
            });

            expect(result.current.chatsSidebarCollapsed).toBe(false);

            act(() => {
                result.current.setChatsSidebarCollapsed(true);
                result.current.setRightPanelOpen(false);
            });

            expect(result.current.chatsSidebarCollapsed).toBe(true);
        });

        it('switching chats leaves the sidebar state alone', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setActiveChatId(1);
                result.current.setChatsSidebarCollapsed(true);
            });

            act(() => {
                result.current.setActiveChatId(2);
            });

            expect(result.current.chatsSidebarCollapsed).toBe(true);
        });
    });
});
