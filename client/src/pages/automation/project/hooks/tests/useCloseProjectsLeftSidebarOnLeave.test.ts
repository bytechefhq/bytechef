import {useCloseProjectsLeftSidebarOnLeave} from '@/pages/automation/project/hooks/useCloseProjectsLeftSidebarOnLeave';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useCloseProjectsLeftSidebarOnLeave', () => {
    beforeEach(() => {
        useProjectsLeftSidebarStore.setState({projectLeftSidebarOpen: true});
    });

    it('keeps the sidebar open while moving between project pages', () => {
        const {rerender} = renderHook(({pathname}) => useCloseProjectsLeftSidebarOnLeave(pathname), {
            initialProps: {pathname: '/automation/projects/1/project-workflows/2'},
        });

        rerender({pathname: '/automation/projects/1/agents/3'});
        rerender({pathname: '/automation/projects/1/data-syncs/4'});
        rerender({pathname: '/automation/projects/5/project-workflows/6'});

        expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(true);
    });

    it('closes the sidebar when leaving the project pages', () => {
        const {rerender} = renderHook(({pathname}) => useCloseProjectsLeftSidebarOnLeave(pathname), {
            initialProps: {pathname: '/automation/projects/1/project-workflows/2'},
        });

        rerender({pathname: '/automation/projects'});

        expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(false);
    });

    it('closes the sidebar on project templates', () => {
        renderHook(() => useCloseProjectsLeftSidebarOnLeave('/automation/projects/1/templates'));

        expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(false);
    });
});
