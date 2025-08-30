import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useParams} from 'react-router-dom';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function CopilotRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const {addMessage, appendToLastAssistantMessage, context, conversationId, messages} = useCopilotStore();
    const {workflow} = useWorkflowDataStore();

    const {projectId, projectWorkflowId} = useParams();

    const queryClient = useQueryClient();

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        addMessage({content: input, role: 'user'});
        setIsRunning(true);

        const result = await fetch('/api/platform/internal/ai/chat', {
            body: JSON.stringify({
                context: {
                    ...context,
                    workflowId: workflow.id,
                },
                message: input,
            }),
            headers: {
                Accept: 'application/x-ndjson',
                'Content-Type': 'application/json',
                'X-Copilot-Conversation-Id': conversationId!,
            },
            method: 'POST',
        });

        if (!result.body) {
            throw new Error('No response body');
        }

        // Prepare an empty assistant message to stream into
        addMessage({content: '', role: 'assistant'});

        let buffer = '';
        const decoder = new TextDecoder('utf-8');
        const reader = result.body.getReader();
        let workflowUpdated = false;

        /* eslint-disable no-constant-condition */
        while (true) {
            const {done, value} = await reader.read();

            if (done) {
                break;
            }

            buffer += decoder.decode(value, {stream: true});

            let index;

            while ((index = buffer.indexOf('\n')) !== -1) {
                const line = buffer.slice(0, index).trim();

                if (!line) {
                    continue;
                }

                buffer = buffer.slice(index + 1);

                try {
                    const obj = JSON.parse(line) as {text?: string; workflowUpdated?: boolean};

                    if (obj.text) {
                        appendToLastAssistantMessage(obj.text);
                    }

                    if (obj.workflowUpdated) {
                        workflowUpdated = true;
                    }
                    /* eslint-disable @typescript-eslint/no-unused-vars */
                } catch (e) {
                    // ignore malformed lines
                }
            }
        }

        // Flush the remaining buffer (in case the stream didn't end with a newline)
        const remaining = buffer.trim();

        if (remaining) {
            try {
                const obj = JSON.parse(remaining) as {text?: string; workflowUpdated?: boolean};

                if (obj.text) {
                    appendToLastAssistantMessage(obj.text);
                }
                if (obj.workflowUpdated) {
                    workflowUpdated = true;
                }
            } catch {
                // ignore
            }
        }

        setIsRunning(false);

        if (workflowUpdated) {
            queryClient.invalidateQueries({
                queryKey: ProjectWorkflowKeys.projectWorkflow(+projectId!, +projectWorkflowId!),
            });
        }
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages,
        onNew,
    });

    return <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>;
}
