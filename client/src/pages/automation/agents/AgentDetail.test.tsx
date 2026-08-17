import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentDetail from './AgentDetail';

const hoisted = vi.hoisted(() => {
    return {
        mockInvalidateAgentQueries: vi.fn(),
        mockRegisterPostTurn: vi.fn(),
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockUseAiAgentQuery: vi.fn(),
        mockUseParams: vi.fn(),
    };
});

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {
        ...actual,
        useParams: hoisted.mockUseParams,
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentQuery: hoisted.mockUseAiAgentQuery,
}));

vi.mock('@/pages/automation/agents/utils/invalidateAgentQueries', () => ({
    default: hoisted.mockInvalidateAgentQueries,
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async () => {
    const actual = await vi.importActual<typeof import('@/shared/components/copilot/stores/useCopilotStore')>(
        '@/shared/components/copilot/stores/useCopilotStore'
    );

    return {
        ...actual,
        useCopilotStore: (selector: (state: {setContext: typeof hoisted.mockSetContext}) => unknown) =>
            selector({setContext: hoisted.mockSetContext}),
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {setCopilotPanelOpen: typeof hoisted.mockSetCopilotPanelOpen}) => unknown) =>
        selector({setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPostTurnRegistry', () => ({
    default: (selector: (state: {register: typeof hoisted.mockRegisterPostTurn}) => unknown) =>
        selector({register: hoisted.mockRegisterPostTurn}),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: true}}}),
}));

vi.mock('@/components/PageLoader', () => ({
    default: ({children, errors, loading}: {children: React.ReactNode; errors: unknown[]; loading: boolean}) =>
        loading ? (
            <div data-testid="page-loader-loading">Loading...</div>
        ) : errors.filter(Boolean).length > 0 ? (
            <div data-testid="page-loader-error">Error</div>
        ) : (
            <div data-testid="page-loader-content">{children}</div>
        ),
}));

vi.mock('./components/detail/AgentDetailHeader', () => ({
    default: ({onAskCopilot, onToggleTestPanel}: {onAskCopilot?: () => void; onToggleTestPanel?: () => void}) => (
        <header data-testid="agent-detail-header">
            {onAskCopilot && (
                <button onClick={onAskCopilot} type="button">
                    Ask Copilot
                </button>
            )}

            <button onClick={onToggleTestPanel} type="button">
                Test
            </button>
        </header>
    ),
}));

vi.mock('./components/detail/AgentInstructionsCard', () => ({default: () => <div data-testid="instructions-card" />}));
vi.mock('./components/detail/AgentModelCard', () => ({default: () => <div data-testid="model-card" />}));
vi.mock('./components/detail/AgentChannelsCard', () => ({default: () => <div data-testid="channels-card" />}));
vi.mock('./components/detail/AgentToolsCard', () => ({default: () => <div data-testid="tools-card" />}));
vi.mock('./components/detail/AgentApprovalsCard', () => ({default: () => <div data-testid="approvals-card" />}));
vi.mock('./components/detail/AgentSkillsCard', () => ({default: () => <div data-testid="skills-card" />}));
vi.mock('./components/detail/AgentSubAgentsCard', () => ({default: () => <div data-testid="sub-agents-card" />}));
vi.mock('./components/detail/AgentKnowledgeBaseCard', () => ({
    default: () => <div data-testid="knowledge-base-card" />,
}));
vi.mock('./components/detail/AgentScheduleCard', () => ({default: () => <div data-testid="schedule-card" />}));
vi.mock('./components/detail/AgentSettingsCard', () => ({default: () => <div data-testid="settings-card" />}));
vi.mock('./components/detail/AgentTestChatPanel', () => ({default: () => <div data-testid="test-chat-panel" />}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({title}: {position?: string; title?: string}) => <header data-testid="sidebar-header">{title}</header>,
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({
        children,
        header,
        rightSidebarBody,
        rightSidebarOpen,
    }: {
        children: React.ReactNode;
        header: React.ReactNode;
        rightSidebarBody?: React.ReactNode;
        rightSidebarOpen?: boolean;
    }) => (
        <div data-testid="layout-container">
            <div data-testid="layout-header">{header}</div>

            <div data-testid="layout-content">{children}</div>

            {rightSidebarOpen && <div data-testid="layout-right-sidebar">{rightSidebarBody}</div>}
        </div>
    ),
}));

const agent = {
    channels: [],
    description: 'A support agent',
    draftWorkflowId: 'workflow-1',
    elements: [],
    id: 'agent-1',
    instructions: null,
    settings: {},
    title: 'Support Agent',
    unpublishedChanges: false,
};

beforeEach(() => {
    hoisted.mockUseParams.mockReturnValue({agentId: 'agent-1'});
    hoisted.mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: agent}, error: null, isLoading: false});
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('AgentDetail', () => {
    it('renders layout container', () => {
        render(<AgentDetail />);

        expect(screen.getByTestId('layout-container')).toBeInTheDocument();
    });

    it('opens copilot scoped to the current agent', async () => {
        render(<AgentDetail />);

        await userEvent.click(screen.getByRole('button', {name: /ask copilot/i}));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith(
            expect.objectContaining({
                mode: MODE.ASK,
                parameters: expect.objectContaining({agentId: 'agent-1'}),
                source: Source.AI_AGENT,
            })
        );
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    // The copilot panel renders over the right rail rather than inside it, so both being open at once is
    // one panel on top of the other rather than two panels side by side.
    it('closes the test panel when the copilot opens', async () => {
        render(<AgentDetail />);

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(screen.getByTestId('layout-right-sidebar')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /ask copilot/i}));

        expect(screen.queryByTestId('layout-right-sidebar')).not.toBeInTheDocument();
    });

    it('closes the copilot when the test panel opens', async () => {
        render(<AgentDetail />);

        await userEvent.click(screen.getByRole('button', {name: 'Test'}));

        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(false);
    });

    it('registers a post-turn callback that invalidates agent queries', () => {
        render(<AgentDetail />);

        expect(hoisted.mockRegisterPostTurn).toHaveBeenCalledWith(Source.AI_AGENT, expect.any(Function));

        const [, callback] = hoisted.mockRegisterPostTurn.mock.calls[0] as [Source, () => void];

        callback();

        expect(hoisted.mockInvalidateAgentQueries).toHaveBeenCalled();
    });
});
