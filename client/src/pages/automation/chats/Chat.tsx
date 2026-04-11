import {Thread} from '@/components/assistant-ui/thread';
import useChat from '@/pages/automation/chats/hooks/useChat';
import {ChatRuntimeProvider} from '@/pages/automation/chats/runtime-providers/ChatRuntimeProvider';

const Chat = () => {
    const {effectiveWorkflowExecutionId, environmentName, sseStreamResponse} = useChat();

    return (
        <div className="flex flex-1">
            <ChatRuntimeProvider
                environmentName={environmentName}
                sseStreamResponse={sseStreamResponse}
                workflowExecutionId={effectiveWorkflowExecutionId}
            >
                <div className="relative flex size-full flex-col">
                    <Thread />
                </div>
            </ChatRuntimeProvider>
        </div>
    );
};

export default Chat;
