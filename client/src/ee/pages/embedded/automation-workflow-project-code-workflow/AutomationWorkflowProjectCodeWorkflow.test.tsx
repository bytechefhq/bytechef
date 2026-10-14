import useCodeWorkflowHeaderStore from '@/pages/platform/code-workflow/stores/useCodeWorkflowHeaderStore';
import {createTestQueryClientWrapper, screen, userEvent} from '@/shared/util/test-utils';
import {act, render} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowProjectCodeWorkflow from './AutomationWorkflowProjectCodeWorkflow';

const hoisted = vi.hoisted(() => ({
    projects: [] as Array<Record<string, unknown>>,
    publishMutate: vi.fn(),
    sourceQuery: vi.fn(),
    updateMutateAsync: vi.fn().mockResolvedValue({}),
}));

vi.mock('@/pages/platform/code-workflow/CodeWorkflowSourceEditor', () => ({
    default: () => <div data-testid="code-workflow-source-editor" />,
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql');

    return {
        ...actual,
        useAutomationWorkflowProjectCodeWorkflowSourceQuery: (...args: unknown[]) => {
            hoisted.sourceQuery(...args);

            return {
                data: {automationWorkflowProjectCodeWorkflowSource: {content: 'source', language: 'JAVASCRIPT'}},
                error: null,
                isLoading: false,
            };
        },
        useAutomationWorkflowProjectsQuery: () => ({
            data: {automationWorkflowProjects: hoisted.projects},
        }),
        usePublishAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutate}),
        useUpdateAutomationWorkflowProjectCodeWorkflowSourceMutation: () => ({
            isPending: false,
            mutateAsync: hoisted.updateMutateAsync,
        }),
    };
});

const buildProject = (overrides: Record<string, unknown> = {}) => ({
    codeWorkflowDraftPublishable: true,
    codeWorkflowLanguage: 'JAVASCRIPT',
    codeWorkflowProject: true,
    id: '7',
    name: 'orders',
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
        hoisted.publishMutate.mockReset();
        hoisted.sourceQuery.mockReset();

        useCodeWorkflowHeaderStore.getState().reset();
    });

    it('renders the editor for a JavaScript code project', () => {
        renderPage();

        expect(screen.getByTestId('code-workflow-source-editor')).toBeInTheDocument();
        expect(screen.queryByTestId('automation-workflows-page')).not.toBeInTheDocument();
    });

    it('redirects to the automation workflows page for a Java code project', () => {
        hoisted.projects = [buildProject({codeWorkflowLanguage: 'JAVA'})];

        renderPage();

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
        expect(screen.queryByTestId('code-workflow-source-editor')).not.toBeInTheDocument();
        expect(hoisted.sourceQuery).toHaveBeenLastCalledWith(expect.anything(), {enabled: false});
    });

    it('redirects to the automation workflows page for a visual project', () => {
        hoisted.projects = [buildProject({codeWorkflowLanguage: null, codeWorkflowProject: false})];

        renderPage();

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
        expect(screen.queryByTestId('code-workflow-source-editor')).not.toBeInTheDocument();
        expect(hoisted.sourceQuery).toHaveBeenLastCalledWith(expect.anything(), {enabled: false});
    });

    it('redirects to the automation workflows page for an unknown project id', () => {
        renderPage('999');

        expect(screen.getByTestId('automation-workflows-page')).toBeInTheDocument();
        expect(screen.queryByTestId('code-workflow-source-editor')).not.toBeInTheDocument();
        expect(hoisted.sourceQuery).toHaveBeenLastCalledWith(expect.anything(), {enabled: false});
    });

    it('shows the project name and publishes it', async () => {
        renderPage();

        expect(screen.getByText('orders')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Publish'}));

        expect(hoisted.publishMutate).toHaveBeenCalledWith({id: '7'}, expect.anything());
    });

    it('disables Publish while there are unsaved edits', () => {
        renderPage();

        act(() => useCodeWorkflowHeaderStore.setState({dirty: true}));

        expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
    });

    it('disables Publish when the draft has no source of its own to publish', () => {
        hoisted.projects = [buildProject({codeWorkflowDraftPublishable: false})];

        renderPage();

        expect(screen.getByRole('button', {name: 'Publish'})).toBeDisabled();
    });

    it('enables Publish when the draft is publishable and there are no unsaved edits', () => {
        renderPage();

        expect(screen.getByRole('button', {name: 'Publish'})).toBeEnabled();
    });
});
