import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubTabsStore, inferDefaultViewMode, useAiHubTabsStore} from '../useAiHubTabsStore';

describe('useAiHubTabsStore', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            tasksSidebarCollapsed: true,
        });
    });

    it('opens a new tab, sets it active, and opens the right panel', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openFileTab('42', 'spec.md');
        });

        expect(result.current.openTabs).toHaveLength(1);

        const tab = result.current.openTabs[0]!;

        expect(tab.kind).toBe('file');

        if (tab.kind === 'file') {
            expect(tab.fileId).toBe('42');
            expect(tab.viewMode).toBe('preview');
        }

        expect(tab.name).toBe('spec.md');
        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('focuses an existing tab when the same fileId is opened again', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('42', 'spec.md');
            result.current.openFileTab('43', 'notes.md');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).not.toBe(firstTabId);

        act(() => {
            result.current.openFileTab('42', 'spec.md');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).toBe(firstTabId);
    });

    it('removes a tab and picks the neighboring tab as active', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';
        let thirdTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('1', 'a.md');
            secondTabId = result.current.openFileTab('2', 'b.md');
            thirdTabId = result.current.openFileTab('3', 'c.md');
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
            tabId = result.current.openFileTab('1', 'a.md');
        });

        act(() => {
            result.current.closeTab(tabId);
        });

        expect(result.current.openTabs).toHaveLength(0);
        expect(result.current.activeTabId).toBeUndefined();
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('updates view mode only for the target file tab', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';
        let secondTabId = '';

        act(() => {
            firstTabId = result.current.openFileTab('1', 'a.md');
            secondTabId = result.current.openFileTab('2', 'b.md');
        });

        act(() => {
            result.current.setViewMode(firstTabId, 'split');
        });

        const firstTab = result.current.openTabs.find((tab) => tab.id === firstTabId)!;
        const secondTab = result.current.openTabs.find((tab) => tab.id === secondTabId)!;

        if (firstTab.kind === 'file') {
            expect(firstTab.viewMode).toBe('split');
        }

        if (secondTab.kind === 'file') {
            expect(secondTab.viewMode).toBe('preview');
        }
    });

    // --- Workflow tab tests ---

    it('opens a workflow tab with kind: workflow and correct fields', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
        });

        expect(result.current.openTabs).toHaveLength(1);

        const tab = result.current.openTabs[0]!;

        expect(tab.kind).toBe('workflow');
        expect(tab.id).toBe(tabId);
        expect(tab.name).toBe('My Workflow');

        if (tab.kind === 'workflow') {
            expect(tab.workflowId).toBe('wf-1');
            expect(tab.projectId).toBe('proj-1');
            expect(tab.projectWorkflowId).toBe(10);
        }

        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('re-points the existing project-scoped workflow tab when another workflow of the same project opens', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
            result.current.openWorkflowTab('wf-2', 'proj-1', 11, 'Other Workflow');
        });

        // Same project → a single tab, re-pointed to the most recently opened workflow (same tab id).
        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(firstTabId);

        const tab = result.current.openTabs[0]!;

        if (tab.kind === 'workflow') {
            expect(tab.workflowId).toBe('wf-2');
            expect(tab.projectWorkflowId).toBe(11);
            expect(tab.name).toBe('Other Workflow');
        }
    });

    // --- DataTable tab tests ---

    it('opens a dataTable tab with kind: dataTable and correct fields', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openDataTableTab('dt-1', 'My Table');
        });

        expect(result.current.openTabs).toHaveLength(1);

        const tab = result.current.openTabs[0]!;

        expect(tab.kind).toBe('dataTable');
        expect(tab.id).toBe(tabId);
        expect(tab.name).toBe('My Table');

        if (tab.kind === 'dataTable') {
            expect(tab.dataTableId).toBe('dt-1');
        }

        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('focuses an existing dataTable tab when the same dataTableId is opened again', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openDataTableTab('dt-1', 'My Table');
            result.current.openDataTableTab('dt-2', 'Other Table');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).not.toBe(firstTabId);

        act(() => {
            result.current.openDataTableTab('dt-1', 'My Table');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).toBe(firstTabId);
    });

    // --- Skill tab tests ---

    it('opens a skill tab and dedups by skillId', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstId = '';

        act(() => {
            firstId = result.current.openSkillTab('7', 'Triage');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.openTabs[0]).toMatchObject({kind: 'skill', name: 'Triage', skillId: '7'});

        act(() => {
            result.current.openSkillTab('7', 'Triage');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(firstId);
    });

    // --- CustomComponent tab tests ---

    it('opens a customComponent tab and dedups by customComponentId', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstId = '';

        act(() => {
            firstId = result.current.openCustomComponentTab('cc-1', 'My Custom Component');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.openTabs[0]).toMatchObject({
            customComponentId: 'cc-1',
            kind: 'customComponent',
            name: 'My Custom Component',
        });

        act(() => {
            result.current.openCustomComponentTab('cc-1', 'My Custom Component');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(firstId);
    });

    // --- CodeWorkflow tab tests ---

    it('opens a codeWorkflow tab and dedups by projectId', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstId = '';

        act(() => {
            firstId = result.current.openCodeWorkflowTab('proj-1', 'java', 'My Code Workflow');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.openTabs[0]).toMatchObject({
            id: 'codeWorkflow-proj-1',
            kind: 'codeWorkflow',
            language: 'java',
            name: 'My Code Workflow',
            projectId: 'proj-1',
        });
        expect(firstId).toBe('codeWorkflow-proj-1');

        act(() => {
            result.current.openCodeWorkflowTab('proj-1', 'java', 'My Code Workflow');
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(firstId);
    });

    // --- KnowledgeBase tab tests ---

    it('opens a knowledgeBase tab with kind: knowledgeBase and correct fields', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let tabId = '';

        act(() => {
            tabId = result.current.openKnowledgeBaseTab('kb-1', 'My KB');
        });

        expect(result.current.openTabs).toHaveLength(1);

        const tab = result.current.openTabs[0]!;

        expect(tab.kind).toBe('knowledgeBase');
        expect(tab.id).toBe(tabId);
        expect(tab.name).toBe('My KB');

        if (tab.kind === 'knowledgeBase') {
            expect(tab.knowledgeBaseId).toBe('kb-1');
        }

        expect(result.current.activeTabId).toBe(tabId);
        expect(result.current.rightPanelOpen).toBe(true);
    });

    it('focuses an existing knowledgeBase tab when the same knowledgeBaseId is opened again', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let firstTabId = '';

        act(() => {
            firstTabId = result.current.openKnowledgeBaseTab('kb-1', 'My KB');
            result.current.openKnowledgeBaseTab('kb-2', 'Other KB');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).not.toBe(firstTabId);

        act(() => {
            result.current.openKnowledgeBaseTab('kb-1', 'My KB');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.activeTabId).toBe(firstTabId);
    });

    // --- Mixed-kind tests ---

    it('can have all four kinds open simultaneously', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        act(() => {
            result.current.openFileTab('f-1', 'readme.md');
            result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
            result.current.openDataTableTab('dt-1', 'My Table');
            result.current.openKnowledgeBaseTab('kb-1', 'My KB');
        });

        expect(result.current.openTabs).toHaveLength(4);

        const kinds = result.current.openTabs.map((tab) => tab.kind);

        expect(kinds).toContain('file');
        expect(kinds).toContain('workflow');
        expect(kinds).toContain('dataTable');
        expect(kinds).toContain('knowledgeBase');
    });

    it('opening a workflow tab after a file tab preserves both', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let fileTabId = '';

        act(() => {
            fileTabId = result.current.openFileTab('f-1', 'readme.md');
        });

        expect(result.current.openTabs).toHaveLength(1);

        act(() => {
            result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
        });

        expect(result.current.openTabs).toHaveLength(2);
        expect(result.current.openTabs[0]!.id).toBe(fileTabId);
        expect(result.current.openTabs[0]!.kind).toBe('file');
        expect(result.current.openTabs[1]!.kind).toBe('workflow');
    });

    it('closing active workflow tab when a file tab exists makes the file tab active', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let fileTabId = '';
        let workflowTabId = '';

        act(() => {
            fileTabId = result.current.openFileTab('f-1', 'readme.md');
            workflowTabId = result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
        });

        expect(result.current.activeTabId).toBe(workflowTabId);

        act(() => {
            result.current.closeTab(workflowTabId);
        });

        expect(result.current.openTabs).toHaveLength(1);
        expect(result.current.activeTabId).toBe(fileTabId);
    });

    // --- setViewMode on non-file tab is a no-op ---

    it('setViewMode on a workflow tab is a no-op', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        let workflowTabId = '';

        act(() => {
            workflowTabId = result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
        });

        const tabsBefore = result.current.openTabs.slice();

        act(() => {
            result.current.setViewMode(workflowTabId, 'split');
        });

        expect(result.current.openTabs).toEqual(tabsBefore);
    });

    // --- inferDefaultViewMode is called only for file tabs ---

    it('non-file tabs do not have a viewMode field (confirming inferDefaultViewMode is not applied)', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        act(() => {
            result.current.openWorkflowTab('wf-1', 'proj-1', 10, 'My Workflow');
            result.current.openDataTableTab('dt-1', 'My Table');
            result.current.openKnowledgeBaseTab('kb-1', 'My KB');
        });

        for (const tab of result.current.openTabs) {
            expect(tab.kind).not.toBe('file');
            expect('viewMode' in tab).toBe(false);
        }
    });

    it('file tabs have viewMode set by inferDefaultViewMode', () => {
        const {result} = renderHook(() => useAiHubTabsStore());

        act(() => {
            result.current.openFileTab('f-1', 'readme.md');
        });

        const tab = result.current.openTabs[0]!;

        expect(tab.kind).toBe('file');

        if (tab.kind === 'file') {
            expect(tab.viewMode).toBe('preview');
        }
    });

    describe('setActiveTaskId — home → task hand-off', () => {
        it('inherits tabs and rightPanelOpen from home view when first task is created', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            // Simulate user attaching a file in the home composer (no active task yet).
            act(() => {
                result.current.openFileTab('42', 'spec.md');
            });

            expect(result.current.activeTaskId).toBeUndefined();
            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);

            // User hits Enter — task is auto-created, transition to a real task id.
            act(() => {
                result.current.setActiveTaskId(1);
            });

            // Tabs and right panel state must carry over — without this, the artifact the user attached
            // on home would be lost when the task panel mounts.
            expect(result.current.activeTaskId).toBe(1);
            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('restores the right panel per task on switch (closed for tasks with no snapshot)', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            // Land in conv-1 with one tab (opening a tab also opens the right panel).
            act(() => {
                result.current.setActiveTaskId(1);
                result.current.openFileTab('42', 'a.md');
            });

            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);

            // Switch to conv-2 — it has no snapshot yet, so it lands with no tabs and the panel closed.
            act(() => {
                result.current.setActiveTaskId(2);
            });

            expect(result.current.openTabs).toHaveLength(0);
            expect(result.current.rightPanelOpen).toBe(false);

            // Switch back to conv-1 — its snapshot is restored as the user left it: the tab is back AND the
            // panel reopens, since the resource-panel state is snapshotted per task.
            act(() => {
                result.current.setActiveTaskId(1);
            });

            expect(result.current.openTabs).toHaveLength(1);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('lets the user re-open the panel after a switch by opening a tab', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setActiveTaskId(1);
                result.current.openFileTab('42', 'a.md');
                result.current.setActiveTaskId(2);
            });

            // Switching closed the panel...
            expect(result.current.rightPanelOpen).toBe(false);

            // ...and opening a tab in the new task re-opens it.
            act(() => {
                result.current.openFileTab('7', 'b.md');
            });

            expect(result.current.rightPanelOpen).toBe(true);
        });
    });

    describe('tasksSidebarCollapsed', () => {
        it('setTasksSidebarCollapsed toggles the rail flag', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            expect(result.current.tasksSidebarCollapsed).toBe(true);

            act(() => {
                result.current.setTasksSidebarCollapsed(false);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(false);

            act(() => {
                result.current.setTasksSidebarCollapsed(true);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(true);
        });

        it('closing the resource panel resets the sidebar back to collapsed', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            // User opened the resource panel, then expanded the sidebar from the rail.
            act(() => {
                result.current.setRightPanelOpen(true);
                result.current.setTasksSidebarCollapsed(false);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(false);

            // Closing the panel must return the sidebar to the rail default so the next open starts collapsed.
            act(() => {
                result.current.setRightPanelOpen(false);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(true);
        });

        it('opening the resource panel preserves the current rail state', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            act(() => {
                result.current.setTasksSidebarCollapsed(false);
            });

            act(() => {
                result.current.setRightPanelOpen(true);
            });

            // setRightPanelOpen(true) must not force the rail state — only closing resets it.
            expect(result.current.tasksSidebarCollapsed).toBe(false);
        });

        it('switching tasks resets the sidebar back to collapsed', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            // On task 1 the user expanded the sidebar over an open resource panel.
            act(() => {
                result.current.setActiveTaskId(1);
                result.current.setRightPanelOpen(true);
                result.current.setTasksSidebarCollapsed(false);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(false);

            // Clicking another task must collapse the sidebar back to the rail — otherwise the full
            // sidebar covers the new task's panels until a page refresh.
            act(() => {
                result.current.setActiveTaskId(2);
            });

            expect(result.current.tasksSidebarCollapsed).toBe(true);
        });
    });

    describe('openWorkflowExecutionTab', () => {
        it('opens a workflowExecution tab with the correct fields and opens the right panel', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            let tabId = '';

            act(() => {
                tabId = result.current.openWorkflowExecutionTab(501, 'Run #501');
            });

            expect(result.current.openTabs).toHaveLength(1);

            const tab = result.current.openTabs[0]!;

            expect(tab.kind).toBe('workflowExecution');
            expect(tab.id).toBe(tabId);
            expect(tab.name).toBe('Run #501');

            if (tab.kind === 'workflowExecution') {
                expect(tab.workflowExecutionId).toBe(501);
            }

            expect(result.current.activeTabId).toBe(tabId);
            expect(result.current.rightPanelOpen).toBe(true);
        });

        it('focuses an existing tab when the same workflowExecutionId is opened again', () => {
            const {result} = renderHook(() => useAiHubTabsStore());

            let firstTabId = '';

            act(() => {
                firstTabId = result.current.openWorkflowExecutionTab(501, 'Run #501');
                result.current.openWorkflowExecutionTab(502, 'Run #502');
            });

            expect(result.current.openTabs).toHaveLength(2);
            expect(result.current.activeTabId).not.toBe(firstTabId);

            act(() => {
                result.current.openWorkflowExecutionTab(501, 'Run #501');
            });

            expect(result.current.openTabs).toHaveLength(2);
            expect(result.current.activeTabId).toBe(firstTabId);
        });
    });

    describe('inferDefaultViewMode', () => {
        it('returns preview for markdown files', () => {
            expect(inferDefaultViewMode('spec.md')).toBe('preview');
            expect(inferDefaultViewMode('notes.MARKDOWN')).toBe('preview');
        });

        it('returns preview for html files', () => {
            expect(inferDefaultViewMode('index.html')).toBe('preview');
            expect(inferDefaultViewMode('page.htm')).toBe('preview');
        });

        it('returns editor for code and data files', () => {
            expect(inferDefaultViewMode('config.json')).toBe('editor');
            expect(inferDefaultViewMode('script.py')).toBe('editor');
            expect(inferDefaultViewMode('data.csv')).toBe('editor');
            expect(inferDefaultViewMode('notes.txt')).toBe('editor');
        });

        it('returns preview for unknown extensions (shows metadata placeholder)', () => {
            expect(inferDefaultViewMode('image.png')).toBe('preview');
            expect(inferDefaultViewMode('archive.zip')).toBe('preview');
        });
    });
});

describe('openWorkflowTab project-scoped dedup', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            activeTaskId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByTaskId: {},
            tasksSidebarCollapsed: true,
        });
    });

    it('opens a single tab per project and re-points it when another workflow of the same project opens', () => {
        const store = aiHubTabsStore.getState();

        store.openWorkflowTab('wf-a', 'project-1', 11, 'Workflow A');
        store.openWorkflowTab('wf-b', 'project-1', 12, 'Workflow B');

        const workflowTabs = aiHubTabsStore.getState().openTabs.filter((tab) => tab.kind === 'workflow');

        expect(workflowTabs).toHaveLength(1);

        const tab = workflowTabs[0]!;

        if (tab.kind === 'workflow') {
            expect(tab.projectId).toBe('project-1');
            expect(tab.workflowId).toBe('wf-b');
            expect(tab.projectWorkflowId).toBe(12);
            expect(tab.name).toBe('Workflow B');
        }
    });

    it('opens separate tabs for workflows from different projects', () => {
        const store = aiHubTabsStore.getState();

        store.openWorkflowTab('wf-a', 'project-1', 11, 'Workflow A');
        store.openWorkflowTab('wf-c', 'project-2', 21, 'Workflow C');

        const workflowTabs = aiHubTabsStore.getState().openTabs.filter((tab) => tab.kind === 'workflow');

        expect(workflowTabs).toHaveLength(2);
    });
});
