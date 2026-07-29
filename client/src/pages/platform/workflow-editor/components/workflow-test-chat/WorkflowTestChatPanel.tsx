import {Thread} from '@/components/assistant-ui/thread';
import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {useWorkflowTestVoiceSession} from '@/shared/hooks/useWorkflowTestVoiceSession';
import {checkVoiceSupport} from '@/shared/lib/browser-voice/BrowserVoiceSession';
import {createWebhookVoiceAdapter} from '@/shared/lib/voice/ByteChefRealtimeVoiceAdapter';
import {VoiceModeLayout} from '@/shared/lib/voice/VoiceModeLayout';
import {AudioLinesIcon, SquareIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface WorkflowTestVoiceModeButtonPropsI {
    active: boolean;
    disabled?: boolean;
    onStart: () => void;
    onStop: () => void;
    title?: string;
}

const WorkflowTestVoiceModeButton = ({active, disabled, onStart, onStop, title}: WorkflowTestVoiceModeButtonPropsI) => {
    return (
        <button
            aria-label={active ? 'Stop voice session' : 'Start voice session'}
            className="inline-flex size-9 items-center justify-center rounded-full hover:bg-muted disabled:cursor-not-allowed disabled:opacity-40"
            disabled={disabled}
            onClick={active ? onStop : onStart}
            title={title}
            type="button"
        >
            {active ? (
                <SquareIcon aria-hidden="true" className="size-4 fill-red-500 text-red-500" />
            ) : (
                <AudioLinesIcon aria-hidden="true" className="size-4" />
            )}
        </button>
    );
};

const WorkflowTestChatPanel = () => {
    const {
        appendToLastAssistantMessage,
        generateConversationId,
        setMessage,
        setWorkflowTestChatPanelOpen,
        workflowTestChatPanelOpen,
    } = useWorkflowTestChatStore(
        useShallow((state) => ({
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            generateConversationId: state.generateConversationId,
            setMessage: state.setMessage,
            setWorkflowTestChatPanelOpen: state.setWorkflowTestChatPanelOpen,
            workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
        }))
    );

    const copilotLayoutShifted = useCopilotLayoutShifted();
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const voiceUnsupportedReason = useMemo(() => checkVoiceSupport(), []);
    const browserSupportsVoice = voiceUnsupportedReason === null;

    // Voice is only available when the workflow has at least one trigger carrying a websocketTasks extension
    // (the embedded sub-workflow that runs during the voice session). Otherwise the voice button would fail
    // at WS-upgrade time with the server's "no websocketTasks" close, which is recoverable but a poor UX.
    const workflowSupportsVoice = (workflow?.triggers ?? []).some((trigger) => {
        const parameters = (trigger?.parameters ?? {}) as Record<string, unknown>;

        return typeof parameters.websocketTasks === 'string' && parameters.websocketTasks.length > 0;
    });

    // A workflow is "voice-only" when its trigger is `browser/v1/voiceSession`. In that case the test panel
    // renders <VoiceModeLayout> through the assistant-ui RealtimeVoiceAdapter instead of the chat <Thread>.
    const isVoiceOnlyWorkflow = useMemo(
        () => (workflow?.triggers ?? []).some((trigger) => trigger?.type === 'browser/v1/voiceSession'),
        [workflow?.triggers]
    );

    // The test panel's voice token endpoint lives at
    // `/api/platform/internal/workflow-tests/{workflowId}/voice-session-token`. `createWebhookVoiceAdapter`
    // appends `/voice-session-token` to the base URL, so we pass the workflow-test base. Only wired for the
    // voice-only path because non-voice-only workflows use `useWorkflowTestVoiceSession` for the inline
    // composer button instead of the assistant-ui adapter.
    const voiceAdapter = useMemo(() => {
        if (!isVoiceOnlyWorkflow || !workflow?.id) {
            return undefined;
        }

        return createWebhookVoiceAdapter(`/api/platform/internal/workflow-tests/${workflow.id}`);
    }, [isVoiceOnlyWorkflow, workflow?.id]);

    const {error, start, status, stop} = useWorkflowTestVoiceSession({
        onEvent: (event) => {
            if (event.type === 'transcript_final' && typeof event.text === 'string' && event.text.length > 0) {
                setMessage({content: event.text, role: 'user'});
            } else if (event.type === 'assistant_text' && typeof event.text === 'string') {
                if (event.done) {
                    return;
                }

                appendToLastAssistantMessage(event.text);
            }
        },
        workflowId: workflow?.id ?? '',
    });

    const voiceActive = status === 'active' || status === 'connecting';

    const handlePanelClose = () => {
        if (voiceActive) {
            stop();
        }

        setWorkflowTestChatPanelOpen(false);
    };

    const handleVoiceStart = useCallback(() => {
        if (!workflow?.id) {
            return;
        }

        setMessage({content: '', role: 'assistant'});

        void start();
    }, [setMessage, start, workflow?.id]);

    const composerActions = useMemo(() => {
        if (!workflow?.id || !workflowSupportsVoice || isVoiceOnlyWorkflow) {
            return undefined;
        }

        return (
            <WorkflowTestVoiceModeButton
                active={voiceActive}
                disabled={!browserSupportsVoice}
                onStart={handleVoiceStart}
                onStop={stop}
                title={!browserSupportsVoice ? (voiceUnsupportedReason ?? undefined) : undefined}
            />
        );
    }, [
        browserSupportsVoice,
        handleVoiceStart,
        isVoiceOnlyWorkflow,
        stop,
        voiceActive,
        voiceUnsupportedReason,
        workflow?.id,
        workflowSupportsVoice,
    ]);

    useEffect(() => {
        // Only refresh conversationId when the panel transitions to open; firing this on every store change
        // (including the voice-mode flip) overwrites the conversationId mid-session, which severs chat memory.
        if (workflowTestChatPanelOpen) {
            generateConversationId();
        }
    }, [generateConversationId, workflowTestChatPanelOpen]);

    if (!workflowTestChatPanelOpen) {
        return <></>;
    }

    return (
        <div
            className={twMerge(
                'absolute inset-y-4 top-2 bottom-6 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background',
                copilotLayoutShifted ? 'right-[57px]' : 'right-[69px]'
            )}
        >
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-surface-main">
                <header className="flex items-center gap-2 p-4 text-lg font-medium">
                    <span>Playground</span>

                    <button
                        aria-label="Close the node details dialog"
                        className="ml-auto pr-0"
                        onClick={handlePanelClose}
                        type="button"
                    >
                        <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                    </button>
                </header>

                {error && <div className="bg-red-50 px-4 py-2 text-xs text-red-900">{error}</div>}

                <div className="absolute inset-x-0 top-16 bottom-0">
                    <WorkflowTestChatRuntimeProvider voiceAdapter={voiceAdapter}>
                        {isVoiceOnlyWorkflow ? (
                            <VoiceModeLayout sessionLimitSeconds={150} />
                        ) : (
                            <Thread composerActions={composerActions} dataComponents={aiChatDataComponents} />
                        )}
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
