import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowTestChatRuntimeProvider} from '@/pages/platform/workflow-editor/components/workflow-test-chat/runtime-providers/WorkflowTestChatRuntimeProvider';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotLayoutShifted from '@/shared/components/copilot/hooks/useCopilotLayoutShifted';
import {MessageSquareXIcon, XIcon} from 'lucide-react';
import {useEffect} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

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

    const copilotLayoutShifted = useCopilotLayoutShifted();

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
                'absolute inset-y-4 top-2 bottom-6 z-10 w-screen max-w-workflow-node-details-panel-width overflow-hidden rounded-lg border border-stroke-neutral-secondary bg-background',
                copilotLayoutShifted ? 'right-[57px]' : 'right-[69px]'
            )}
        >
            <div className="flex h-full flex-col divide-y divide-gray-100 bg-surface-main">
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
                    <WorkflowTestChatRuntimeProvider>
                        <Thread />
                    </WorkflowTestChatRuntimeProvider>
                </div>
            </div>
        </div>
    );
};

export default WorkflowTestChatPanel;
