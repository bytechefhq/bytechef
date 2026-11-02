import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import Chats from '../Chats';

const hoisted = vi.hoisted(() => ({
    mockUseWorkspaceChatWorkflowsQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: undefined}),
    useWorkspaceChatWorkflowsQuery: hoisted.mockUseWorkspaceChatWorkflowsQuery,
}));

vi.mock('@/pages/automation/journey/useAutomationJourney', async () => {
    const {getAutomationJourney} = await vi.importActual<
        typeof import('@/pages/automation/journey/getAutomationJourney')
    >('@/pages/automation/journey/getAutomationJourney');

    return {useAutomationJourney: () => ({isLoading: false, journey: getAutomationJourney([], [])})};
});

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        leftSidebarHidden,
    }: {
        children: ReactNode;
        header: ReactNode;
        leftSidebarHidden?: boolean;
    }) => (
        <div data-left-sidebar-hidden={String(!!leftSidebarHidden)} data-testid="layout-container">
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {title?: ReactNode}) => <header data-testid="header-title">{title}</header>,
}));

vi.mock('@/pages/automation/chats/components/ChatsSidebar', () => ({
    default: () => null,
}));

const renderChats = () =>
    render(
        <MemoryRouter initialEntries={['/automation/chats']}>
            <Routes>
                <Route element={<Chats />} path="/automation/chats" />
            </Routes>
        </MemoryRouter>
    );

describe('Chats', () => {
    beforeEach(() => {
        hoisted.mockUseWorkspaceChatWorkflowsQuery.mockReset();
    });

    it('hides the sidebar and explains how a workflow becomes a chat when there are none', () => {
        hoisted.mockUseWorkspaceChatWorkflowsQuery.mockReturnValue({
            data: {workspaceChatWorkflows: []},
            isLoading: false,
        });

        renderChats();

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'true');
        expect(screen.getByTestId('header-title')).toHaveTextContent('Chats');
        expect(screen.getByText('No chat workflows yet')).toBeInTheDocument();
        expect(screen.getByText('How chats work')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /Go to Projects/})).toBeInTheDocument();
        expect(screen.getByText('Create a project').closest('li')).toHaveAttribute('aria-current', 'step');
        expect(screen.getByText('Deploy and enable')).toBeInTheDocument();
    });

    it('keeps the sidebar and asks to pick a chat when chat workflows exist', () => {
        hoisted.mockUseWorkspaceChatWorkflowsQuery.mockReturnValue({
            data: {
                workspaceChatWorkflows: [
                    {
                        projectDeploymentId: '1',
                        projectId: '2',
                        projectName: 'Support',
                        workflowExecutionId: 'abc',
                        workflowLabel: 'Help desk',
                    },
                ],
            },
            isLoading: false,
        });

        renderChats();

        expect(screen.getByTestId('layout-container')).toHaveAttribute('data-left-sidebar-hidden', 'false');
        expect(screen.getByText('Select a chat')).toBeInTheDocument();
        expect(screen.queryByText('No chat workflows yet')).not.toBeInTheDocument();
    });
});
