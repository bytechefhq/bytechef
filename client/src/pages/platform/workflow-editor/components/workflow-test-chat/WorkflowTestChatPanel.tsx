import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {createWebhookVoiceAdapter} from '@/shared/lib/voice/ByteChefRealtimeVoiceAdapter';
import {VoiceModeLayout} from '@/shared/lib/voice/VoiceModeLayout';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {MessageSquareXIcon, XIcon} from 'lucide-react';
import {useEffect, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const DEFAULT_SESSION_LIMIT_SECONDS = 150;

const SERVER_MAX_SESSION_LIMIT_SECONDS = 30 * 60;

const WorkflowTestChatPanel = () => {
    const {generateConversationId, resetMessages, setWorkflowTestChatPanelOpen, workflowTestChatPanelOpen} =
        useWorkflowTestChatStore(
            useShallow((state) => ({
                generateConversationId: state.generateConversationId,
                resetMessages: state.resetMessages,
                setWorkflowTestChatPanelOpen: state.setWorkflowTestChatPanelOpen,
                workflowTestChatPanelOpen: state.workflowTestChatPanelOpen,
            }))
        );

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const copilotLayoutShifted = useCopilotLayoutShifted();

    const voiceTrigger = useMemo(
        () => (workflow?.triggers ?? []).find((trigger) => trigger?.type === 'browser/v1/voiceSession'),
        [workflow?.triggers]
    );
    const hasVoiceAgent = !!(voiceTrigger?.clusterElements as {voiceAgent?: unknown} | undefined)?.voiceAgent;
    const isVoiceOnlyWorkflow = !!voiceTrigger;

    const voiceAdapter = useMemo(() => {
        if (!hasVoiceAgent || !workflow?.id) {
            return undefined;
        }

        const sampleRate = Number((voiceTrigger?.parameters as {sampleRate?: unknown} | undefined)?.sampleRate);

        return createWebhookVoiceAdapter(
            `/api/platform/internal/workflow-tests/${workflow.id}`,
            sampleRate === 16000 || sampleRate === 24000 ? sampleRate : undefined,
            {environmentId: String(currentEnvironmentId)}
        );
    }, [currentEnvironmentId, hasVoiceAgent, voiceTrigger?.parameters, workflow?.id]);

    const voiceSessionLimitSeconds = useMemo(() => {
        const sessionLimitSeconds = (voiceTrigger?.parameters as {sessionLimitSeconds?: unknown} | undefined)
            ?.sessionLimitSeconds;

        if (typeof sessionLimitSeconds !== 'number' || Number.isNaN(sessionLimitSeconds)) {
            return DEFAULT_SESSION_LIMIT_SECONDS;
        }

        return sessionLimitSeconds > 0 ? sessionLimitSeconds : SERVER_MAX_SESSION_LIMIT_SECONDS;
    }, [voiceTrigger?.parameters]);

    const handlePanelClose = () => {
        setWorkflowTestChatPanelOpen(false);
    };

    const handleResetChatClick = () => {
        resetMessages();

        generateConversationId();
    };

    useEffect(() => {
        generateConversationId();
    }, [generateConversationId, workflowTestChatPanelOpen]);

    if (!workflowTestChatPanelOpen) {
        return <></>;
    }

    return (
        <div
            className={twMerge(
                'absolute inset-y-4 top-[15px] bottom-6 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background',
                copilotLayoutShifted ? 'right-[57px]' : 'right-[69px]'
            )}
        >
            <div className="flex h-full flex-col divide-y divide-stroke-neutral-secondary bg-surface-main">
                <header className="flex items-center p-4 text-lg font-medium">
                    <span>Playground</span>

                    <div className="ml-auto flex items-center gap-2">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Reset the conversation"
                                    icon={<MessageSquareXIcon />}
                                    onClick={handleResetChatClick}
                                    size="iconSm"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Reset conversation</TooltipContent>
                        </Tooltip>

                        <Button
                            aria-label="Close the playground panel"
                            icon={<XIcon />}
                            onClick={handlePanelClose}
                            size="iconSm"
                            variant="ghost"
                        />
                    </div>
                </header>

                <div className="absolute inset-x-0 top-16 bottom-0">
                    <WorkflowTestChatRuntimeProvider voiceAdapter={voiceAdapter}>
                        {isVoiceOnlyWorkflow ? (
                            hasVoiceAgent ? (
                                <VoiceModeLayout sessionLimitSeconds={voiceSessionLimitSeconds} />
                            ) : (
                                <div className="flex h-full items-center justify-center p-8 text-center text-sm text-muted-foreground">
                                    Add a Voice Agent to the trigger to test with voice
                                </div>
                            )
                        ) : (
                            <Thread />
                        )}
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
