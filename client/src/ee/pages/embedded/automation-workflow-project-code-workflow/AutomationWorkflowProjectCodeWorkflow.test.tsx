import {createTestQueryClientWrapper, screen} from '@/shared/util/test-utils';
import {render} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectCodeWorkflow from './AutomationWorkflowProjectCodeWorkflow';

const hoisted = vi.hoisted(() => ({
    projects: [] as Array<Record<string, unknown>>,
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAutomationWorkflowProjectsQuery: () => ({
            data: {automationWorkflowProjects: hoisted.projects},
        }),
    };
});

const buildProject = (overrides: Record<string, unknown> = {}) => ({
    codeWorkflowLanguage: 'JAVASCRIPT',
    codeWorkflowProject: true,
    id: '7',
    name: 'orders',
    workflowTemplates: [{workflowUuid: 'uuid-1'}, {workflowUuid: 'uuid-2'}],
    ...overrides,
});

const renderPage = (projectId = '7') => {
    const QueryClientWrapper = createTestQueryClientWrapper();

    const wrapper = ({children}: {children: ReactNode}) => <QueryClientWrapper>{children}</QueryClientWrapper>;

    return render(
        <MemoryRouter initialEntries={[`/embedded/automation-workflows/projects/${projectId}/code`]}>
            <Routes>
                <Route
                    element={<AutomationWorkflowProjectCodeWorkflow />}
                    path="/embedded/automation-workflows/projects/:automationWorkflowProjectId/code"
                />

                <Route
                    element={<div data-testid="automation-workflow-editor" />}
                    path="/embedded/automation-workflows/:workflowUuid/editor"
                />

                <Route
                    element={<div data-testid="automation-workflows-page" />}
                    path="/embedded/automation-workflows"
                />
            </Routes>
        </MemoryRouter>,
        {wrapper}
    );
};

describe('AutomationWorkflowProjectCodeWorkflow', () => {
    beforeEach(() => {
        hoisted.projects = [buildProject()];
    });

    it('opens the first workflow of a JavaScript code project in the workflow editor', () => {
        renderPage();

        expect(screen.getByTestId('automation-workflow-editor')).toBeInTheDocument();
    });

    it('redirects to the automation workflows page for a Java code project', () => {
        hoisted.projects = [buildProject({codeWorkflowLanguage: 'JAVA'})];

        renderPage();

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
    });

    it('redirects to the automation workflows page for a visual project', () => {
        hoisted.projects = [buildProject({codeWorkflowLanguage: null, codeWorkflowProject: false})];

        renderPage();

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
    });

    it('redirects to the automation workflows page for a code project without workflows', () => {
        hoisted.projects = [buildProject({workflowTemplates: []})];

        renderPage();

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
    });

    it('redirects to the automation workflows page for an unknown project id', () => {
        renderPage('999');

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
    });
});
