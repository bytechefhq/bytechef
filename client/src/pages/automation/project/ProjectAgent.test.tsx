import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectAgent from './ProjectAgent';

const hoisted = vi.hoisted(() => ({
    openCopilot: vi.fn(),
}));

vi.mock('@/shared/components/copilot/hooks/useOpenCopilot', () => ({
    default: () => hoisted.openCopilot,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
    useParams: () => ({agentId: '22', projectId: '7'}),
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar', () => ({
    default: ({projectId}: {projectId: number}) => <aside data-testid="projects-sidebar">{projectId}</aside>,
}));

vi.mock('@/pages/automation/agents/AgentDetailContent', () => ({
    default: ({agentId}: {agentId: string}) => <div data-testid="agent-detail-content">{agentId}</div>,
}));

vi.mock('@/pages/automation/agents/components/detail/AgentDetailHeader', () => ({
    default: ({
        leading,
        onAskCopilot,
        onToggleTestPanel,
        testPanelOpen,
    }: {
        leading?: ReactNode;
        onAskCopilot?: () => void;
        onToggleTestPanel: () => void;
        testPanelOpen: boolean;
    }) => (
        <header>
            {leading}

            <button onClick={onToggleTestPanel}>{testPanelOpen ? 'Hide Test Agent panel' : 'Test Agent'}</button>

            {onAskCopilot && <button onClick={onAskCopilot}>Ask Copilot</button>}
        </header>
    ),
}));

const mockUseElementWidth = vi.fn(() => 1200);

vi.mock('@/shared/hooks/useElementWidth', () => ({
    default: () => mockUseElementWidth(),
}));

const mockProjectItemSelect = vi.fn();

vi.mock('@/pages/automation/project/components/project-header/components/ProjectItemSelect', () => ({
    default: (props: {currentAgentId: string; currentLabel: string; projectId: number}) => {
        mockProjectItemSelect(props);

        return <span data-testid="project-item-select">{props.currentLabel}</span>;
    },
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetProjectQuery: () => ({
        data: {id: 7, lastProjectVersion: 3, lastStatus: ProjectStatus.Draft, name: 'Support Project', workspaceId: 1},
    }),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/shared/layout/LeftSidebarButton', () => ({
    default: ({onLeftSidebarOpenClick}: {onLeftSidebarOpenClick: () => void}) => (
        <button aria-label="Toggle project sidebar" onClick={onLeftSidebarOpenClick} />
    ),
}));

vi.mock('@/pages/automation/agents/components/detail/AgentTestChatPanel', () => ({
    default: () => <div data-testid="agent-test-chat" />,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentQuery: () => ({
        data: {
            aiAgent: {draftWorkflowId: 'wf', id: '22', lastPublishedVersion: 0, projectId: '7', title: 'Support Bot'},
        },
    }),
}));

const renderProjectAgent = () => render(<ProjectAgent />, {wrapper: TooltipProvider});

describe('ProjectAgent', () => {
    it('renders the agent inside the project layout', () => {
        renderProjectAgent();

        expect(screen.getByTestId('projects-sidebar')).toHaveTextContent('7');
        expect(screen.getByTestId('agent-detail-content')).toHaveTextContent('22');
        expect(screen.getByText('Support Bot')).toBeInTheDocument();
    });

    it('opens the AI Agent Copilot on the open agent from the header', () => {
        renderProjectAgent();

        fireEvent.click(screen.getByText('Ask Copilot'));

        expect(hoisted.openCopilot).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {aiAgentId: '22'},
            source: Source.AI_AGENT,
        });
    });

    it('renders an item switcher for the header title, scoped to the agent and its project', () => {
        renderProjectAgent();

        expect(screen.getByTestId('project-item-select')).toHaveTextContent('Support Bot');
        expect(mockProjectItemSelect).toHaveBeenCalledWith(
            expect.objectContaining({currentAgentId: '22', currentLabel: 'Support Bot', projectId: 7})
        );
    });

    it('shows the project name and project version, and no agent-only version badge', () => {
        renderProjectAgent();

        expect(screen.getByText('Support Project')).toBeInTheDocument();
        expect(screen.getByText('V3')).toBeInTheDocument();
        expect(screen.getByText('DRAFT')).toBeInTheDocument();

        expect(screen.getAllByText('DRAFT')).toHaveLength(1);
    });

    it('puts the sidebar toggle and the project breadcrumb before the agent switcher', () => {
        useProjectsLeftSidebarStore.setState({projectLeftSidebarOpen: false});

        renderProjectAgent();

        expect(screen.getByText('Support Project')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle project sidebar'}));

        expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(true);
    });

    describe('test panel sizing', () => {
        beforeEach(() => {
            mockUseElementWidth.mockReturnValue(1200);
            useProjectsLeftSidebarStore.setState({projectLeftSidebarOpen: true});
        });

        it('gives the test panel its max width when there is plenty of room', () => {
            renderProjectAgent();

            expect(screen.getByTestId('agent-test-chat').parentElement).toHaveStyle({width: '580px'});
            expect(screen.getByRole('button', {name: 'Hide Test Agent panel'})).toBeInTheDocument();
        });

        it('shrinks the test panel, but keeps the form at its minimum width, when room is limited', () => {
            mockUseElementWidth.mockReturnValue(900);

            renderProjectAgent();

            expect(screen.getByTestId('agent-test-chat').parentElement).toHaveStyle({width: '456px'});
            expect(screen.getByTestId('agent-detail-content').parentElement).toHaveClass('min-w-[420px]');
        });

        it('hides the test panel when there is not enough room, and clicking Test collapses the sidebar', () => {
            mockUseElementWidth.mockReturnValue(700);

            const {rerender} = renderProjectAgent();

            expect(screen.queryByTestId('agent-test-chat')).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Test Agent'})).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button', {name: 'Test Agent'}));

            expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(false);

            mockUseElementWidth.mockReturnValue(1200);

            rerender(<ProjectAgent />);

            expect(screen.getByTestId('agent-test-chat').parentElement).toHaveStyle({width: '580px'});
        });
    });
});
