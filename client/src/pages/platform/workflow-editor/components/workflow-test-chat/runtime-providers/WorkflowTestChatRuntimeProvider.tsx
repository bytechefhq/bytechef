import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useWorkflowTestStream} from '@/shared/hooks/useWorkflowTestStream';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getTestWorkflowStreamPostRequest} from '@/shared/util/testWorkflow-utils';
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
import {useShallow} from 'zustand/react/shallow';

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => {
    return message;
};

export function WorkflowTestChatRuntimeProvider({
    children,
}: Readonly<{
    children: ReactNode;
}>) {
    const [isRunning, setIsRunning] = useState(false);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {setWorkflowIsRunning} = useWorkflowEditorStore(
        useShallow((state) => ({
            setWorkflowIsRunning: state.setWorkflowIsRunning,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow!);
    const {conversationId, messages, setMessage} = useWorkflowTestChatStore(
        useShallow((state) => ({
            conversationId: state.conversationId,
            messages: state.messages,
            setMessage: state.setMessage,
        }))
    );

    const {setStreamRequest} = useWorkflowTestStream({
        onError: () => setIsRunning(false),
        onResult: () => setIsRunning(false),
        workflowId: workflow.id!,
    });

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
        setIsRunning(true);
        setWorkflowIsRunning(true);

        try {
            const request = getTestWorkflowStreamPostRequest({
                environmentId: currentEnvironmentId,
                id: workflow.id!,
                testWorkflowRequest: {
                    inputs: {
                        trigger_1: {
                            attachments: message.attachments,
                            conversationId,
                            message: input,
                        },
                    },
                },
            });

            setStreamRequest(request);
        } catch (error) {
            console.error('Failed to build test workflow stream request:', error);

            setIsRunning(false);
            setWorkflowIsRunning(false);
        }
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
