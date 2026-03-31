import { ChangeEvent } from 'react';
import { MODE, useCopilotStore, Source } from "@/shared/components/copilot/stores/useCopilotStore";
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import { getCookie } from '@/shared/util/cookie-utils';
import { getRandomId } from '@/shared/util/random-utils';
import { HttpAgent } from '@ag-ui/client';

const handleImportN8nWorkflow = async (event: ChangeEvent<HTMLInputElement>) => {
    if (!event.target.files) return;

    const file = event.target.files[0];
    if (!file) return;

    try {
        const fileText = await file.text();
        const workflowJson = JSON.parse(fileText);

        const copilotStore = useCopilotStore.getState();
        const workflow = useWorkflowDataStore.getState().workflow;
        const currentComponent = useWorkflowNodeDetailsPanelStore.getState().currentComponent;

        copilotStore.resetMessages();
        copilotStore.generateConversationId();

        copilotStore.setContext({
            ...copilotStore.context,
            mode: MODE.BUILD,
            parameters: workflowJson,
            source: Source.CONVERTER,
        });

        const messageContent = JSON.stringify(workflowJson, null, 2);

        copilotStore.addMessage({
            role: 'user',
            content: messageContent,
        });

        const agent = new HttpAgent({
            agentId: Source[Source.CONVERTER],
            headers: {
                'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '',
            },
            threadId: copilotStore.conversationId!,
            url: `/api/platform/internal/ai/chat/${Source[Source.CONVERTER].toLowerCase()}`,
        });

        agent.addMessage({
            content: messageContent,
            id: getRandomId(),
            role: 'user',
        });

        const { workflowExecutionError, ...contextWithoutError } = copilotStore.context ?? {};

        const stateToSend = {
            ...contextWithoutError,
            currentSelectedNode: currentComponent?.name,
            workflowId: workflow.id,
        };

        agent.setState(stateToSend);

        copilotStore.addMessage({
            role: 'assistant',
            content: '',
        });

        const subscriber = {
            onTextMessageContentEvent: ({ textMessageBuffer }: any) => {
                copilotStore.appendToLastAssistantMessage(textMessageBuffer);
            },
        };

        await agent.runAgent(
            { runId: getRandomId() },
            subscriber
        );

        console.log('✅ Imported workflow JSON sent and executed in Copilot');

    } catch (err) {
        console.error('❌ Failed to read or parse file', err);
    }
};

export default handleImportN8nWorkflow;
