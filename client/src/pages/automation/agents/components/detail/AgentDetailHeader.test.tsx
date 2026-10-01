import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AgentDetailHeader from './AgentDetailHeader';

const hoisted = vi.hoisted(() => ({
    publishProjectMutate: vi.fn(),
}));

vi.mock('@/pages/automation/agents/components/AgentDialog', () => ({default: () => null}));
vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => null,
}));
vi.mock('@/pages/automation/project/components/ProjectVersionHistorySheet', () => ({default: () => null}));
vi.mock('@/pages/automation/project/components/project-header/components/PublishPopover', () => ({
    default: ({
        onPublishProjectSubmit,
        title,
    }: {
        onPublishProjectSubmit: ({description, onSuccess}: {description?: string; onSuccess: () => void}) => void;
        title: string;
    }) => (
        <button onClick={() => onPublishProjectSubmit({description: 'First release', onSuccess: () => {}})}>
            {title}
        </button>
    ),
}));

const mockSettingsMenu = vi.fn();

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu', () => ({
    default: (props: {
        firstTab?: {
            ariaLabel: string;
            content: (onCloseDropdownMenu: () => void) => ReactNode;
            label: string;
            value: string;
        };
        project: {id?: number};
    }) => {
        mockSettingsMenu(props);

        return <div data-testid="settings-menu" />;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

const mockDeleteAiAgentMutate = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentVersionsQuery: () => ({data: undefined}),
    useDeleteAiAgentMutation: () => ({isPending: false, mutate: mockDeleteAiAgentMutate}),
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    usePublishProjectMutation: () => ({isPending: false, mutate: hoisted.publishProjectMutate}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <TooltipProvider>{ui}</TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const renderHeader = (lastPublishedVersion: number, project?: {id: number; name: string; workspaceId: number}) =>
    wrap(
        <AgentDetailHeader
            id="agent-1"
            lastPublishedVersion={lastPublishedVersion}
            onToggleTestPanel={vi.fn()}
            project={project}
            projectId="7"
            testPanelOpen={false}
            title="Agent1"
        />
    );

describe('AgentDetailHeader', () => {
    it('does not render an agent-only version badge', () => {
        renderHeader(3);

        expect(screen.queryByText('DRAFT')).not.toBeInTheDocument();
        expect(screen.queryByText('V4')).not.toBeInTheDocument();
    });

    it('shows the status dot and a settings menu with an Agent tab once the project has loaded', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        expect(screen.getByLabelText('Loading indicator')).toBeInTheDocument();

        expect(screen.getByTestId('settings-menu')).toBeInTheDocument();
        expect(mockSettingsMenu).toHaveBeenCalledWith(
            expect.objectContaining({
                firstTab: expect.objectContaining({ariaLabel: 'Agent tab', label: 'Agent', value: 'agent'}),
                project: {id: 7, name: 'Support Project', workspaceId: 1},
            })
        );
    });

    it('does not render the settings menu before the project has loaded', () => {
        renderHeader(0);

        expect(screen.queryByTestId('settings-menu')).not.toBeInTheDocument();
    });

    it('no longer renders its own agent menu', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        expect(screen.queryByLabelText('Agent menu')).not.toBeInTheDocument();
    });

    it('wires the settings menu Agent tab to delete the agent through the shared agent-actions mutation, guarded by a confirmation dialog', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        const {firstTab} = mockSettingsMenu.mock.calls.at(-1)![0] as {
            firstTab: {content: (onCloseDropdownMenu: () => void) => ReactNode};
        };

        wrap(firstTab.content(vi.fn()));

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(mockDeleteAiAgentMutate).not.toHaveBeenCalled();
        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Agent Deletion'}));

        expect(mockDeleteAiAgentMutate).toHaveBeenCalledWith({id: 'agent-1'});
    });

    it('publishes the agent project through the project publish', () => {
        renderHeader(0);

        fireEvent.click(screen.getByRole('button', {name: 'Publish Project'}));

        expect(hoisted.publishProjectMutate).toHaveBeenCalledWith(
            {id: 7, publishProjectRequest: {description: 'First release'}},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });
});
