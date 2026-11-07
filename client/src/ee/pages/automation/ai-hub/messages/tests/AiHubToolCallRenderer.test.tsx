import AiHubToolCallRenderer, {
    ARTIFACT_OPEN_TOOL_NAMES,
    AiHubToolCallFallback,
} from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {type ToolCallMessagePartProps} from '@assistant-ui/react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

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
        useAiHubSettingsStore.setState({showToolCalls: false});
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
        it('is one of the artifact-open tool names', () => {
            expect([...ARTIFACT_OPEN_TOOL_NAMES]).toEqual([
                'openAiAgentTab',
                'openCodeWorkflowTab',
                'openCustomComponentTab',
                'openWorkflowTab',
            ]);
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

    describe('openAiAgentTab', () => {
        it('renders as a clickable AI Agent link that opens the agent tab', () => {
            render(
                <AiHubToolCallRenderer
                    args={{aiAgentId: '42', name: 'Support Agent'}}
                    result={{opened: true}}
                    toolCallId="call-agent"
                    toolName="openAiAgentTab"
                />
            );

            expect(screen.getByText('Support Agent')).toBeInTheDocument();
            expect(screen.getByText('AI Agent')).toBeInTheDocument();

            fireEvent.click(screen.getByRole('button'));

            expect(aiHubTabsStore.getState().openTabs[0]).toMatchObject({
                aiAgentId: '42',
                kind: 'aiAgent',
                name: 'Support Agent',
            });
        });
    });
});

describe('AiHubToolCallRenderer code resources', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({openTabs: []});
    });

    it('renders openCustomComponentTab as a link that opens the custom component tab', () => {
        render(
            <AiHubToolCallRenderer
                args={{customComponentId: 'cc-1', name: 'My Component'}}
                result={{opened: true}}
                toolCallId="call-cc"
                toolName="openCustomComponentTab"
            />
        );

        expect(screen.getByText('Custom Component')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button'));

        expect(aiHubTabsStore.getState().openTabs[0]).toMatchObject({
            customComponentId: 'cc-1',
            kind: 'customComponent',
            name: 'My Component',
        });
    });

    it('renders openCodeWorkflowTab as a link that opens the code workflow tab', () => {
        render(
            <AiHubToolCallRenderer
                args={{language: 'PYTHON', name: 'My Code Workflow', projectId: '11'}}
                result={{opened: true}}
                toolCallId="call-cw"
                toolName="openCodeWorkflowTab"
            />
        );

        expect(screen.getByText('Code Workflow')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button'));

        expect(aiHubTabsStore.getState().openTabs[0]).toMatchObject({
            kind: 'codeWorkflow',
            language: 'PYTHON',
            name: 'My Code Workflow',
            projectId: '11',
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

    beforeEach(() => {
        useAiHubSettingsStore.setState({showToolCalls: false});
    });

    it('hides ordinary tool cards while tool calls are hidden', () => {
        const {container} = renderFallback('listWorkflows');

        expect(container).toBeEmptyDOMElement();
    });

    it('shows ordinary tool cards once tool calls are shown', () => {
        useAiHubSettingsStore.setState({showToolCalls: true});

        renderFallback('listWorkflows');

        expect(screen.getByText('listWorkflows')).toBeInTheDocument();
    });

    it('always shows the workflow link card', () => {
        renderFallback('openWorkflowTab', WORKFLOW_ARGS);

        expect(screen.getByText('Lead Scoring')).toBeInTheDocument();
    });

    describe('running indicator while tool calls are hidden', () => {
        const renderInFlight = (toolName: string) =>
            render(
                <AiHubToolCallFallback
                    {...({args: {}, toolCallId: 'call-1', toolName} as unknown as ToolCallMessagePartProps)}
                />
            );

        beforeEach(() => {
            aiChatToolCallStore.setState({order: [], toolCalls: {}});
        });

        it('shows a building row for a running buildWorkflow call', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

            renderInFlight('buildWorkflow');

            expect(screen.getByRole('status')).toHaveTextContent('Building workflow...');
        });

        it('shows an importing row for a running importWorkflow call', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'importWorkflow', 0);

            renderInFlight('importWorkflow');

            expect(screen.getByRole('status')).toHaveTextContent('Importing workflow...');
        });

        it('shows a researching row for a running research call', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'research', 0);

            renderInFlight('research');

            expect(screen.getByRole('status')).toHaveTextContent('Researching the web...');
        });

        it.each([
            ['data_analyst', 'Analyzing data...'],
            ['image_generator', 'Generating image...'],
            ['slide_builder', 'Building slides...'],
        ])('shows a labeled row for a running %s call', (toolName, label) => {
            aiChatToolCallStore.getState().startToolCall('call-1', toolName, 0);

            renderInFlight(toolName);

            expect(screen.getByRole('status')).toHaveTextContent(label);
        });

        it('shows a generic running row for any other running call', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'listWorkflows', 0);

            renderInFlight('listWorkflows');

            expect(screen.getByRole('status')).toHaveTextContent('Running listWorkflows...');
        });

        it('removes the row once the call completes', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

            const {container} = renderInFlight('buildWorkflow');

            act(() => {
                aiChatToolCallStore.getState().completeToolCall('call-1', 'done', false);
            });

            expect(container).toBeEmptyDOMElement();
        });

        it('removes the row once the call fails', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

            const {container} = renderInFlight('buildWorkflow');

            act(() => {
                aiChatToolCallStore.getState().completeToolCall('call-1', {error: 'boom'}, true);
            });

            expect(container).toBeEmptyDOMElement();
        });

        it('removes the row once the call is aborted', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);

            const {container} = renderInFlight('buildWorkflow');

            act(() => {
                aiChatToolCallStore.getState().failAllRunning({reason: 'Cancelled by user'}, 'aborted');
            });

            expect(container).toBeEmptyDOMElement();
        });

        it('shows nothing for a restored call that has no live entry', () => {
            const {container} = renderInFlight('buildWorkflow');

            expect(container).toBeEmptyDOMElement();
        });

        it('replaces the building label with the latest delegate step as it arrives', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
            aiChatToolCallStore.getState().startProgress('call-1', 'Build a Gmail to Slack workflow');

            renderInFlight('buildWorkflow');

            expect(screen.getByRole('status')).toHaveTextContent('Building workflow...');

            act(() => {
                aiChatToolCallStore.getState().addProgress('call-1', 'Searching components');
                aiChatToolCallStore.getState().addProgress('call-1', 'Saving the workflow');
            });

            expect(screen.getByText('Saving the workflow')).toHaveClass('font-medium');
            expect(screen.queryByText('Building workflow...')).toBeNull();
            expect(screen.getByRole('button', {expanded: false, name: /1 earlier step/})).toBeInTheDocument();
        });

        it('clears the progress once the delegate reports done', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
            aiChatToolCallStore.getState().addProgress('call-1', 'Saving the workflow');

            const {container} = renderInFlight('buildWorkflow');

            act(() => {
                aiChatToolCallStore.getState().finishProgress('call-1', 'done');
            });

            expect(container).toBeEmptyDOMElement();
        });

        it('keeps a failed delegate visible after the call completes', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
            aiChatToolCallStore.getState().addProgress('call-1', 'Saving the workflow');

            renderInFlight('buildWorkflow');

            act(() => {
                aiChatToolCallStore.getState().finishProgress('call-1', 'failed');
                aiChatToolCallStore.getState().completeToolCall('call-1', {error: 'boom'}, true);
            });

            expect(screen.getByRole('status')).toHaveTextContent('Failed');
            expect(screen.getByRole('status')).toHaveTextContent('at Saving the workflow');
        });
    });

    describe('delegate progress on a shown tool card', () => {
        beforeEach(() => {
            aiChatToolCallStore.setState({order: [], toolCalls: {}});
            useAiHubSettingsStore.setState({showToolCalls: true});
        });

        it('renders the latest step under the card header without expanding the card', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'buildWorkflow', 0);
            aiChatToolCallStore.getState().addProgress('call-1', 'Configuring "slack › sendChannelMessage"');

            render(<AiHubToolCallRenderer toolCallId="call-1" toolName="buildWorkflow" />);

            expect(screen.getByRole('button', {expanded: false})).toBeInTheDocument();
            expect(screen.getByRole('status')).toHaveTextContent('Configuring "slack › sendChannelMessage"');
        });

        it('renders no progress for a running call that reported none', () => {
            aiChatToolCallStore.getState().startToolCall('call-1', 'listWorkflows', 0);

            render(<AiHubToolCallRenderer toolCallId="call-1" toolName="listWorkflows" />);

            expect(screen.queryByRole('status')).toBeNull();
        });
    });
});
