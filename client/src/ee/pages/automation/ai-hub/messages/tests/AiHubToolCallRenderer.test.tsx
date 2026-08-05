import AiHubToolCallRenderer from '@/ee/pages/automation/ai-hub/messages/AiHubToolCallRenderer';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {mockGetProject, mockToastError} = vi.hoisted(() => ({
    mockGetProject: vi.fn(),
    mockToastError: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', async () => {
    const actual = await vi.importActual<Record<string, unknown>>('@/shared/middleware/automation/configuration');

    class MockProjectApi {
        getProject = mockGetProject;
    }

    return {
        ...actual,
        ProjectApi: MockProjectApi,
    };
});

vi.mock('sonner', () => ({
    toast: {
        error: mockToastError,
    },
}));

describe('AiHubToolCallRenderer', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
        mockGetProject.mockReset();
        mockToastError.mockReset();
    });

    it('renders the tool name in the header', () => {
        render(<AiHubToolCallRenderer toolCallId="call-1" toolName="getFile" />);

        expect(screen.getByText('getFile')).toBeInTheDocument();
    });

    it('renders an args summary in the header when args are present', () => {
        render(
            <AiHubToolCallRenderer
                args={{name: 'spec.md'}}
                result={{ok: true}}
                toolCallId="call-1"
                toolName="openFile"
            />
        );

        expect(screen.getByText(/name: spec.md/i)).toBeInTheDocument();
    });

    it('starts collapsed for unknown tool names and expands on click', () => {
        render(
            <AiHubToolCallRenderer
                args={{path: 'README.md'}}
                result={{ok: true}}
                toolCallId="call-1"
                toolName="readFile"
            />
        );

        // The body label "Input" should not be visible while collapsed.
        expect(screen.queryByText('Input')).toBeNull();

        fireEvent.click(screen.getByRole('button', {expanded: false}));

        expect(screen.getByText('Input')).toBeInTheDocument();
    });

    it('renders the running spinner when no result is available', () => {
        const {container} = render(<AiHubToolCallRenderer toolCallId="call-1" toolName="research" />);

        expect(container.querySelector('.animate-spin')).not.toBeNull();
    });

    it('renders an error icon (not the success/running icon) when isError=true', () => {
        const {container} = render(
            <AiHubToolCallRenderer isError result={{error: 'boom'}} toolCallId="call-1" toolName="createFile" />
        );

        // Behavioral check: the status maps to the alert-circle icon (lucide's icon-identity
        // class), not the check-circle (success) or loader (running) icon. Asserting icon
        // identity — real, observable behavior — instead of a bare color-utility class string
        // catches selection bugs that a class-string assertion would miss entirely: the previous
        // version of this test asserted `.text-content-error-primary`, a class that never emitted
        // any CSS (no matching `--content-error-primary` token exists), so the test kept passing
        // while the error icon silently rendered uncolored.
        const alertIcon = container.querySelector('.lucide-circle-alert');

        expect(alertIcon).not.toBeNull();
        expect(container.querySelector('.lucide-circle-check')).toBeNull();
        expect(container.querySelector('.lucide-loader-circle')).toBeNull();

        // Guard against regressing to the removed dead class, and pin the real token now in use.
        expect(container.querySelector('.text-content-error-primary')).toBeNull();
        expect(alertIcon).toHaveClass('text-content-destructive-primary');
    });

    it('renders runChatWorkflow with per-step sections from the store progressive output', () => {
        aiChatToolCallStore.getState().startToolCall('call-rcw', 'runChatWorkflow', 0);
        aiChatToolCallStore.getState().appendProgressiveOutput('call-rcw', 'Step 1 ran fine\n\nStep 2 also ran');

        render(<AiHubToolCallRenderer toolCallId="call-rcw" toolName="runChatWorkflow" />);

        // runChatWorkflow auto-expands.
        expect(screen.getByText(/Step 1$/)).toBeInTheDocument();
        expect(screen.getByText(/Step 2$/)).toBeInTheDocument();
        expect(screen.getByText(/Step 1 ran fine/)).toBeInTheDocument();
        expect(screen.getByText(/Step 2 also ran/)).toBeInTheDocument();
    });

    it('renders a subagent tool call as a progress breadcrumb', () => {
        aiChatToolCallStore.getState().startToolCall('call-r', 'research', 0);
        aiChatToolCallStore.getState().addProgress('call-r', 'Searching the web');
        aiChatToolCallStore.getState().addProgress('call-r', 'Synthesizing answer');

        render(<AiHubToolCallRenderer toolCallId="call-r" toolName="research" />);

        expect(screen.getByText('Searching the web')).toBeInTheDocument();
        expect(screen.getByText('Synthesizing answer')).toBeInTheDocument();
        expect(screen.getByText('subagent')).toBeInTheDocument();
    });

    it('renders memory tool call with name and type badge', () => {
        render(
            <AiHubToolCallRenderer
                args={{name: 'Standup notes', type: 'TEXT'}}
                result={{ok: true}}
                toolCallId="call-m"
                toolName="createMemory"
            />
        );

        // memory badge appears in header.
        expect(screen.getByText('memory')).toBeInTheDocument();

        // expand the card to see the name + type body.
        fireEvent.click(screen.getByRole('button', {expanded: false}));

        expect(screen.getByText('Standup notes')).toBeInTheDocument();
        expect(screen.getByText('TEXT')).toBeInTheDocument();
    });

    it('subagent renders a starting placeholder when no progress events have arrived yet', () => {
        aiChatToolCallStore.getState().startToolCall('call-r', 'workflowBuilder', 0);

        render(<AiHubToolCallRenderer toolCallId="call-r" toolName="workflowBuilder" />);

        expect(screen.getByText(/Subagent is starting/i)).toBeInTheDocument();
    });

    it('runChatWorkflow shows a waiting placeholder when no output has arrived', () => {
        aiChatToolCallStore.getState().startToolCall('call-rcw', 'runChatWorkflow', 0);

        render(<AiHubToolCallRenderer toolCallId="call-rcw" toolName="runChatWorkflow" />);

        expect(screen.getByText(/Waiting for workflow output/i)).toBeInTheDocument();
    });

    describe('openCustomComponentTab', () => {
        it('renders as a clickable artifact link instead of a JSON card', () => {
            render(
                <AiHubToolCallRenderer
                    args={{customComponentId: 'cc-1', name: 'My Component'}}
                    result={{opened: true}}
                    toolCallId="call-cc"
                    toolName="openCustomComponentTab"
                />
            );

            expect(screen.getByText('My Component')).toBeInTheDocument();
            expect(screen.getByText('Custom Component')).toBeInTheDocument();
            expect(screen.queryByText('Input')).toBeNull();
        });

        it('dispatches openCustomComponentTab on click', () => {
            render(
                <AiHubToolCallRenderer
                    args={{customComponentId: 'cc-1', name: 'My Component'}}
                    result={{opened: true}}
                    toolCallId="call-cc"
                    toolName="openCustomComponentTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            const openTabs = aiHubTabsStore.getState().openTabs;

            expect(openTabs).toHaveLength(1);
            expect(openTabs[0]).toMatchObject({
                customComponentId: 'cc-1',
                kind: 'customComponent',
                name: 'My Component',
            });
        });
    });

    describe('openCodeWorkflowTab', () => {
        it('renders as a clickable artifact link instead of a JSON card', () => {
            render(
                <AiHubToolCallRenderer
                    args={{language: 'PYTHON', name: 'My Code Workflow', projectId: '11'}}
                    result={{opened: true}}
                    toolCallId="call-cw"
                    toolName="openCodeWorkflowTab"
                />
            );

            expect(screen.getByText('My Code Workflow')).toBeInTheDocument();
            expect(screen.getByText('Code Workflow')).toBeInTheDocument();
            expect(screen.queryByText('Input')).toBeNull();
        });

        it('dispatches openCodeWorkflowTab directly when args carry a language', () => {
            render(
                <AiHubToolCallRenderer
                    args={{language: 'PYTHON', name: 'My Code Workflow', projectId: '11'}}
                    result={{opened: true}}
                    toolCallId="call-cw"
                    toolName="openCodeWorkflowTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            const openTabs = aiHubTabsStore.getState().openTabs;

            expect(openTabs).toHaveLength(1);
            expect(openTabs[0]).toMatchObject({
                kind: 'codeWorkflow',
                language: 'PYTHON',
                name: 'My Code Workflow',
                projectId: '11',
            });
            expect(mockGetProject).not.toHaveBeenCalled();
        });

        it('fetches the project language and opens the tab when args have no language (rehydrated card)', async () => {
            mockGetProject.mockResolvedValue({codeWorkflowLanguage: 'JAVASCRIPT', id: 1});

            render(
                <AiHubToolCallRenderer
                    args={{name: 'My Code Workflow', projectId: '11'}}
                    result={{opened: true}}
                    toolCallId="call-cw"
                    toolName="openCodeWorkflowTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            expect(mockGetProject).toHaveBeenCalledWith({id: 11});

            await waitFor(() => expect(aiHubTabsStore.getState().openTabs).toHaveLength(1));

            const openedTab = aiHubTabsStore.getState().openTabs[0]!;

            expect(openedTab).toMatchObject({
                kind: 'codeWorkflow',
                language: 'JAVASCRIPT',
                name: 'My Code Workflow',
                projectId: '11',
            });
            expect(mockToastError).not.toHaveBeenCalled();
        });

        it('surfaces a toast and does not open a tab when the project fetch fails', async () => {
            mockGetProject.mockRejectedValue(new Error('network down'));

            render(
                <AiHubToolCallRenderer
                    args={{name: 'My Code Workflow', projectId: '11'}}
                    result={{opened: true}}
                    toolCallId="call-cw"
                    toolName="openCodeWorkflowTab"
                />
            );

            fireEvent.click(screen.getByRole('button'));

            await waitFor(() => expect(mockToastError).toHaveBeenCalledWith(expect.stringContaining('network down')));

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
        });
    });
});
