import {useAutomationEditorSidebarStore} from '@/ee/pages/embedded/automation/components/automation-editor/stores/useAutomationEditorSidebarStore';
import {expect, it} from 'vitest';

it('keeps the sidebar closed by default', () => {
    expect(useAutomationEditorSidebarStore.getState().leftSidebarOpen).toBe(false);
});

it('remembers the open state across sessions', () => {
    useAutomationEditorSidebarStore.getState().setLeftSidebarOpen(true);

    expect(JSON.parse(window.localStorage.getItem('bytechef.automation-editor-sidebar')!).state).toEqual({
        leftSidebarOpen: true,
    });
});
