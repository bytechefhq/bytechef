import { useCallback, useState } from 'react';
import { HttpAgent, AgentSubscriber } from '@ag-ui/client';
import { getCookie } from '@/shared/util/cookie-utils';
import { getRandomId } from '@/shared/util/random-utils';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';

export const useConvertN8nToWorkflow = () => {
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
    const setWorkflow = useWorkflowDataStore((state) => state.setWorkflow);
    const [isRunning, setIsRunning] = useState(false);

    const extractJson = (text: string) => {
        const firstBrace = text.indexOf('{');
        const lastBrace = text.lastIndexOf('}');
        if (firstBrace === -1 || lastBrace === -1) {
            throw new Error('No JSON found in response');
        }
        return text.slice(firstBrace, lastBrace + 1);
    };

    const convertN8nWorkflow = useCallback(async (workflowJson: string) => {
        const json = JSON.parse(workflowJson);
        const messageContent = JSON.stringify(json, null, 2);

        const agent = new HttpAgent({
            agentId: 'CONVERTER',
            headers: { 'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || '' },
            threadId: getRandomId(),
            url: `/api/platform/internal/ai/chat/converter`,
        });

        agent.addMessage({
            content: messageContent,
            id: getRandomId(),
            role: 'user',
        });

        agent.setState({
            currentSelectedNode: currentComponent?.name,
            workflowId: workflow.id,
        });

        let convertedWorkflowBuffer = '';
        setIsRunning(true);

        const subscriber: AgentSubscriber = {
            onTextMessageContentEvent: ({ event }) => {
                convertedWorkflowBuffer += event.delta ?? '';
            },
            onTextMessageEndEvent: () => {
                try {
                    const cleaned = extractJson(convertedWorkflowBuffer);
                    const parsedWorkflow = JSON.parse(cleaned);
                    setWorkflow(parsedWorkflow);
                } catch (e) {
                    console.error('Failed to parse workflow', e);
                } finally {
                    setIsRunning(false);
                }
            },
        };

        await agent.runAgent({ runId: getRandomId() }, subscriber);
        return convertedWorkflowBuffer;
    }, [workflow.id, currentComponent?.name, setWorkflow]);

    return { convertN8nWorkflow, isRunning };
};


