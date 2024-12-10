import type {ChatModelAdapter} from '@assistant-ui/react';

const CopilotChatModelAdapter: ChatModelAdapter = {
    async run({abortSignal, messages}) {
        const result = await fetch('/api/platform/internal/ai/chat', {
            // forward the last message in the chat to the API
            body: JSON.stringify({
                message: messages[messages.length - 1],
            }),
            headers: {
                'Content-Type': 'application/json',
                'X-Copilot-Conversation-Id': sessionStorage.getItem('bytechef.copilot-conversation-id') ?? '',
            },
            method: 'POST',
            // if the user hits the "cancel" button or escape keyboard key, cancel the request
            signal: abortSignal,
        });

        const responses: {text: string}[] = await result.json();

        return {
            content: [
                {
                    text: responses.map((message) => message.text).join(''),
                    type: 'text',
                },
            ],
        };
    },
};

export default CopilotChatModelAdapter;
