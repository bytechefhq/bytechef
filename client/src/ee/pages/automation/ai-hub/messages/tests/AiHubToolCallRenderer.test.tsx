import AiHubToolCallRenderer, {
    ARTIFACT_OPEN_TOOL_NAMES,
    AiHubToolCallFallback,
    AiHubToolGroup,
    AiHubToolGroupHeader,
} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {type ToolCallMessagePartProps} from '@assistant-ui/react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    messageContentRef: {current: [] as Array<Record<string, unknown>>},
}));

vi.mock('@assistant-ui/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@assistant-ui/react')>()),
    useAuiState: (selector: (state: {message: {content: unknown}}) => unknown) =>
        selector({message: {content: hoisted.messageContentRef.current}}),
}));

const WORKFLOW_ARGS = {name: 'Lead Scoring', projectId: '5', projectWorkflowId: 12, workflowId: 'wf-1'};

describe('AiHubToolCallRenderer', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
            snapshotsByChatId: {},
        });
    });

    it('renders the tool name in the header', () => {
        render(<AiHubToolCallRenderer toolCallId="call-1" toolName="listWorkflows" />);

        expect(screen.getByText('listWorkflows')).toBeInTheDocument();
    });

    it('renders an args summary in the header when args are present', () => {
        render(
            <AiHubToolCallRenderer
                args={{name: 'spec'}}
                result={{ok: true}}
                toolCallId="call-1"
                toolName="getWorkflow"
            />
        );

        expect(screen.getByText(/name: spec/i)).toBeInTheDocument();
    });

    it('starts collapsed and expands on click', () => {
        render(
            <AiHubToolCallRenderer
                args={{projectId: 5}}
                result={{ok: true}}
                toolCallId="call-1"
                toolName="listWorkflows"
            />
        );

        expect(screen.queryByText('Input')).toBeNull();

        fireEvent.click(screen.getByRole('button', {expanded: false}));

        expect(screen.getByText('Input')).toBeInTheDocument();
        expect(screen.getByText('Result')).toBeInTheDocument();
    });

    it('renders the running spinner when no result is available', () => {
        const {container} = render(<AiHubToolCallRenderer toolCallId="call-1" toolName="buildWorkflow" />);

        expect(container.querySelector('.animate-spin')).not.toBeNull();
    });

    it('follows the live store entry over the static props', () => {
        aiChatToolCallStore.getState().startToolCall('call-live', 'buildWorkflow', 0);

        const {container} = render(
            <AiHubToolCallRenderer result={{ok: true}} toolCallId="call-live" toolName="buildWorkflow" />
        );

        expect(container.querySelector('.animate-spin')).not.toBeNull();
    });

    it('renders an error icon when isError=true', () => {
        const {container} = render(
            <AiHubToolCallRenderer isError result={{error: 'boom'}} toolCallId="call-1" toolName="createProject" />
        );

        const alertIcon = container.querySelector('.lucide-circle-alert');

        expect(alertIcon).not.toBeNull();
        expect(container.querySelector('.lucide-circle-check')).toBeNull();
        expect(alertIcon).toHaveClass('text-content-destructive-primary');
    });

    describe('openWorkflowTab', () => {
        it('is the only artifact-open tool name', () => {
            expect([...ARTIFACT_OPEN_TOOL_NAMES]).toEqual(['openWorkflowTab']);
        });

        it('renders as a clickable workflow link instead of a JSON card', () => {
            render(
                <AiHubToolCallRenderer
                    args={WORKFLOW_ARGS}
                    result={{opened: true}}
                    toolCallId="call-wf"
                    toolName="openWorkflowTab"
                />
            );

            expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
            expect(screen.getByText('Workflow')).toBeInTheDocument();
            expect(screen.queryByText('Input')).toBeNull();
        });

        it('opens the workflow tab on click', () => {
            render(
                <AiHubToolCallRenderer
                    args={WORKFLOW_ARGS}
                    result={{opened: true}}
                    toolCallId="call-wf"
                    toolName="openWorkflowTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            const openTabs = aiHubTabsStore.getState().openTabs;

            expect(openTabs).toHaveLength(1);
            expect(openTabs[0]).toMatchObject({kind: 'workflow', ...WORKFLOW_ARGS});
            expect(aiHubTabsStore.getState().rightPanelOpen).toBe(true);
        });

        it('opens nothing when the args lack the project', () => {
            render(
                <AiHubToolCallRenderer
                    args={{name: 'Lead Scoring', workflowId: 'wf-1'}}
                    result={{opened: true}}
                    toolCallId="call-wf"
                    toolName="openWorkflowTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
        });
    });
});

describe('AiHubToolCallFallback', () => {
    const renderFallback = (toolName: string, args: Record<string, unknown> = {}) =>
        render(
            <AiHubToolCallFallback
                {...({args, result: {ok: true}, toolCallId: 'call-1', toolName} as unknown as ToolCallMessagePartProps)}
            />
        );

    it('renders an ordinary tool row outside a tool group', () => {
        renderFallback('listWorkflows');

        expect(screen.getByText('listWorkflows')).toBeInTheDocument();
    });

    it('always shows the workflow link card', () => {
        renderFallback('openWorkflowTab', WORKFLOW_ARGS);

        expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
    });
});

describe('AiHubToolGroupHeader', () => {
    const renderHeader = (toolParts: Array<{toolCallId: string; toolName: string}>) =>
        render(<AiHubToolGroupHeader expanded={false} onToggle={() => {}} toolParts={toolParts} />);

    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
    });

    it('labels a running buildWorkflow call', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

        renderHeader([{toolCallId: 'call-1', toolName: 'buildWorkflow'}]);

        expect(screen.getByRole('status')).toHaveTextContent('Building workflow...');
    });

    it('labels a running importWorkflow call', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'importWorkflow', 0);

        renderHeader([{toolCallId: 'call-1', toolName: 'importWorkflow'}]);

        expect(screen.getByRole('status')).toHaveTextContent('Importing workflow...');
    });

    it('labels any other running call generically', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'listWorkflows', 0);

        renderHeader([{toolCallId: 'call-1', toolName: 'listWorkflows'}]);

        expect(screen.getByRole('status')).toHaveTextContent('Running listWorkflows...');
    });

    it('counts the calls once none is running', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

        renderHeader([
            {toolCallId: 'call-1', toolName: 'buildWorkflow'},
            {toolCallId: 'call-2', toolName: 'listWorkflows'},
        ]);

        act(() => {
            aiChatToolCallStore.getState().completeToolCall('call-1', 'done', false);
        });

        expect(screen.queryByRole('status')).toBeNull();
        expect(screen.getByRole('button', {expanded: false})).toHaveTextContent('Ran 2 tools');
    });

    it('drops the running label once the call is aborted', () => {
        aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

        renderHeader([{toolCallId: 'call-1', toolName: 'buildWorkflow'}]);

        act(() => {
            aiChatToolCallStore.getState().failAllRunning({reason: 'Cancelled by user'}, 'aborted');
        });

        expect(screen.queryByRole('status')).toBeNull();
        expect(screen.getByRole('button')).toHaveTextContent('Ran 1 tool');
    });
});

describe('AiHubToolGroup', () => {
    const renderGroup = (toolNames: string[]) => {
        hoisted.messageContentRef.current = toolNames.map((toolName, index) => ({
            args: toolName === 'openWorkflowTab' ? WORKFLOW_ARGS : {},
            result: {ok: true},
            toolCallId: `call-${index}`,
            toolName,
            type: 'tool-call',
        }));

        return render(
            <AiHubToolGroup endIndex={toolNames.length - 1} startIndex={0}>
                {toolNames.map((toolName, index) => (
                    <AiHubToolCallFallback
                        key={toolName}
                        {...({
                            args: toolName === 'openWorkflowTab' ? WORKFLOW_ARGS : {},
                            result: {ok: true},
                            toolCallId: `call-${index}`,
                            toolName,
                        } as unknown as ToolCallMessagePartProps)}
                    />
                ))}
            </AiHubToolGroup>
        );
    };

    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
    });

    it('collapses its calls into a single marker', () => {
        renderGroup(['listWorkflows', 'getWorkflow']);

        expect(screen.getByRole('button', {expanded: false})).toHaveTextContent('Ran 2 tools');
        expect(screen.queryByText('listWorkflows')).toBeNull();
        expect(screen.queryByText('getWorkflow')).toBeNull();
    });

    it('lists its calls once the marker is clicked', () => {
        renderGroup(['listWorkflows', 'getWorkflow']);

        fireEvent.click(screen.getByRole('button', {expanded: false}));

        expect(screen.getByText('listWorkflows')).toBeInTheDocument();
        expect(screen.getByText('getWorkflow')).toBeInTheDocument();
    });

    it('keeps the workflow link visible and leaves it out of the count', () => {
        renderGroup(['listWorkflows', 'openWorkflowTab']);

        expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
        expect(screen.getByRole('button', {expanded: false})).toHaveTextContent('Ran 1 tool');
    });

    it('renders no marker for a group of workflow links only', () => {
        renderGroup(['openWorkflowTab']);

        expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
        expect(screen.queryByText(/Ran \d/)).toBeNull();
    });
});
