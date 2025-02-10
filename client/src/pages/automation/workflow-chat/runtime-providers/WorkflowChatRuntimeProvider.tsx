import {useWorkflowChatStore} from '@/pages/automation/workflow-chat/stores/useWorkflowChatStore';
import {
    AppendMessage,
    AssistantRuntimeProvider,
    CompositeAttachmentAdapter,
    SimpleImageAttachmentAdapter,
    SimpleTextAttachmentAdapter,
    ThreadMessageLike,
    useExternalStoreRuntime,
} from '@assistant-ui/react';
import {ReactNode, useState} from 'react';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function WorkflowTestChatRuntimeProvider({
    children,
    environment,
    workflowExecutionId,
}: Readonly<{
    environment: string;
    workflowExecutionId: string;
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {conversationId, messages, setMessage} = useWorkflowChatStore();

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        setMessage({attachments: [...message.attachments], content: input, role: 'user'});
        setIsRunning(true);

        const formData = new FormData();

        formData.append('conversationId', conversationId ?? '');
        formData.append('message', input ?? '');

        for (const attachment of message.attachments) {
            if (attachment.file) {
                formData.append('attachments', attachment.file, attachment.name);
            }
        }

        const result = await fetch('/webhooks/' + workflowExecutionId, {
            body: formData,
            headers: {
                'X-Environment': environment,
            },
            method: 'POST',
            // if the user hits the "cancel" button or escape keyboard key, cancel the request
            // signal: abortSignal,
        }).then(async (res) => {
            if (res.status >= 400) {
                const result = await res.json();

                return {
                    error: {
                        detail: result.detail,
                        message: 'An error occurred',
                    },
                };
            } else {
                return res.json();
            }
        });

        setMessage({
            content: result.message ?? result.error.message + '\n' + result.error.detail,
            role: 'assistant',
        });
        setIsRunning(false);
    };

    const runtime = useExternalStoreRuntime({
        adapters: {
            attachments: new CompositeAttachmentAdapter([
                new SimpleImageAttachmentAdapter(),
                new SimpleTextAttachmentAdapter(),
            ]),
        },
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
