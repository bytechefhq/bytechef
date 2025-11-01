import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {WorkflowTestApi} from '@/shared/middleware/platform/workflow/test';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
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

const workflowTestApi = new WorkflowTestApi();

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
    const {setWorkflowIsRunning, setWorkflowTestExecution} = useWorkflowEditorStore(
        useShallow((state) => ({
            setWorkflowIsRunning: state.setWorkflowIsRunning,
            setWorkflowTestExecution: state.setWorkflowTestExecution,
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

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        setMessage({attachments: [...(message.attachments ?? [])], content: input, role: 'user'});
        setIsRunning(true);
        setWorkflowIsRunning(true);

        const workflowTestExecution = await workflowTestApi
            .testWorkflow({
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
            })
            .catch((error) => error);

        setMessage({
            content: workflowTestExecution.job?.outputs?.message ?? '',
            role: 'assistant',
        });
        setIsRunning(false);
        setWorkflowTestExecution(workflowTestExecution);
        setWorkflowIsRunning(false);
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
