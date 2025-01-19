import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {ReactNode, useState} from 'react';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, context, conversationId, messages} = useCopilotStore();

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        setIsRunning(true);

        const result = await fetch('/api/platform/internal/ai/chat', {
            body: JSON.stringify({
                context,
                message: input,
            }),
            headers: {
                'Content-Type': 'application/json',
                'X-Copilot-Conversation-Id': conversationId!,
            },
            method: 'POST',
            // if the user hits the "cancel" button or escape keyboard key, cancel the request
            // signal: abortSignal,
        });

        const responses: {text: string}[] = await result.json();

        addMessage({
            content: responses.map((message) => message.text).join(''),
            role: 'assistant',
        });
        setIsRunning(false);
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
