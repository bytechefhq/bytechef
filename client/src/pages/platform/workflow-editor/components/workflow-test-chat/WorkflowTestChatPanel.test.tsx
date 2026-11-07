import {TooltipProvider} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowTestChatPanel from './WorkflowTestChatPanel';

const {createWebhookVoiceAdapterMock} = vi.hoisted(() => ({
    createWebhookVoiceAdapterMock: vi.fn(),
}));

vi.mock('@/components/assistant-ui/thread', () => ({Thread: () => <div data-testid="thread" />}));
vi.mock(
    '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider',
    () => ({WorkflowTestChatRuntimeProvider: ({children}: {children: React.ReactNode}) => <>{children}</>})
);
vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({default: () => false}));
vi.mock('@/shared/lib/voice/ByteChefRealtimeVoiceAdapter', () => ({
    createWebhookVoiceAdapter: createWebhookVoiceAdapterMock,
}));
vi.mock('@/shared/lib/voice/VoiceModeLayout', () => ({
    VoiceModeLayout: ({sessionLimitSeconds}: {sessionLimitSeconds: number}) => (
        <div data-session-limit-seconds={sessionLimitSeconds} data-testid="voice-mode-layout" />
    ),
}));

describe('WorkflowTestChatPanel', () => {
    beforeEach(() => {
        useWorkflowDataStore.setState({workflow: {definition: '{}', id: 'workflow-1'}} as Parameters<
            typeof useWorkflowDataStore.setState
        >[0]);
        useWorkflowTestChatStore.setState({
            conversationId: 'conversation-1',
            messages: [{content: [{text: 'hi', type: 'text'}], role: 'user'}],
            workflowTestChatPanelOpen: true,
        });
    });

    it('clears the thread and starts a new conversation on Reset conversation', () => {
        render(
            <TooltipProvider>
                <WorkflowTestChatPanel />
            </TooltipProvider>
        );

        fireEvent.click(screen.getByLabelText('Reset the conversation'));

        const {conversationId, messages} = useWorkflowTestChatStore.getState();

        expect(messages).toEqual([]);
        expect(conversationId).toBeDefined();
        expect(conversationId).not.toBe('conversation-1');
    });

    it('offers voice when the browser trigger has a Voice Agent', () => {
        const triggers = [
            {
                clusterElements: {
                    voiceAgent: {name: 'voiceAgent_1', parameters: {}, type: 'openai/v1/voiceAgent'},
                },
                name: 'trigger_1',
                type: 'browser/v1/voiceSession',
            },
        ] as WorkflowTrigger[];

        useWorkflowDataStore.setState({workflow: {definition: '{}', id: 'workflow-1', triggers}} as Parameters<
            typeof useWorkflowDataStore.setState
        >[0]);

        render(
            <TooltipProvider>
                <WorkflowTestChatPanel />
            </TooltipProvider>
        );

        expect(screen.getByTestId('voice-mode-layout')).toBeInTheDocument();
        expect(screen.queryByText('Add a Voice Agent to the trigger to test with voice')).not.toBeInTheDocument();
    });

    it('passes the current environment and the trigger session limit to the voice session', () => {
        const triggers = [
            {
                clusterElements: {
                    voiceAgent: {name: 'voiceAgent_1', parameters: {}, type: 'openai/v1/voiceAgent'},
                },
                name: 'trigger_1',
                parameters: {sessionLimitSeconds: 90},
                type: 'browser/v1/voiceSession',
            },
        ] as WorkflowTrigger[];

        environmentStore.setState({currentEnvironmentId: 3});
        useWorkflowDataStore.setState({workflow: {definition: '{}', id: 'workflow-1', triggers}} as Parameters<
            typeof useWorkflowDataStore.setState
        >[0]);

        render(
            <TooltipProvider>
                <WorkflowTestChatPanel />
            </TooltipProvider>
        );

        expect(createWebhookVoiceAdapterMock).toHaveBeenLastCalledWith(
            '/api/platform/internal/workflow-tests/workflow-1',
            undefined,
            {environmentId: '3'}
        );
        expect(screen.getByTestId('voice-mode-layout')).toHaveAttribute('data-session-limit-seconds', '90');
    });

    it('asks for a Voice Agent when the slot is empty', () => {
        const triggers = [{name: 'trigger_1', type: 'browser/v1/voiceSession'}] as WorkflowTrigger[];

        useWorkflowDataStore.setState({workflow: {definition: '{}', id: 'workflow-1', triggers}} as Parameters<
            typeof useWorkflowDataStore.setState
        >[0]);

        render(
            <TooltipProvider>
                <WorkflowTestChatPanel />
            </TooltipProvider>
        );

        expect(screen.getByText('Add a Voice Agent to the trigger to test with voice')).toBeInTheDocument();
        expect(screen.queryByTestId('voice-mode-layout')).not.toBeInTheDocument();
    });
});
