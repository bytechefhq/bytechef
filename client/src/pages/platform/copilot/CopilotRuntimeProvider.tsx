'use client';

import {AssistantRuntimeProvider, type ChatModelAdapter, useLocalRuntime} from '@assistant-ui/react';

import type {ReactNode} from 'react';

const MyModelAdapter: ChatModelAdapter = {
    async run({abortSignal, messages}) {
        // TODO replace with your own API
        const result = await fetch('/api/platform/internal/ai/chat', {
            // forward the last message in the chat to the API
            body: JSON.stringify({
                message: messages[messages.length - 1],
            }),
            headers: {
                'Content-Type': 'application/json',
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

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const runtime = useLocalRuntime(MyModelAdapter);

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
