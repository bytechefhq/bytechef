import {Thread} from '@/components/assistant-ui/thread';
import useChat from '@/pages/automation/chats/hooks/useChat';
import {ChatRuntimeProvider} from '@/pages/automation/chats/runtime-providers/ChatRuntimeProvider';
import {useChatsStore} from '@/pages/automation/chats/stores/useChatsStore';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import {Loader2Icon} from 'lucide-react';
import {useShallow} from 'zustand/shallow';

const Chat = () => {
    const {currentStepName, isRunning} = useChatsStore(
        useShallow((state) => ({
            currentStepName: state.currentStepName,
            isRunning: state.isRunning,
        }))
    );

    const {effectiveWorkflowExecutionId, environmentName, sseStreamResponse} = useChat();

    return (
        <div className="flex flex-1">
            <ChatRuntimeProvider
                environmentName={environmentName}
                sseStreamResponse={sseStreamResponse}
                workflowExecutionId={effectiveWorkflowExecutionId}
            >
                <div className="relative flex size-full flex-col">
                    {isRunning && currentStepName && (
                        <div className="absolute top-2 left-1/2 z-10 flex -translate-x-1/2 items-center gap-1.5 rounded-full border bg-background px-3 py-1 text-xs text-muted-foreground shadow-sm">
                            <Loader2Icon className="size-3 animate-spin" />

                            <span>Running {currentStepName}…</span>
                        </div>
                    )}

                    <Thread dataComponents={aiChatDataComponents} />
                </div>
            </ChatRuntimeProvider>
        </div>
    );
};

export default Chat;
