import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {useCallback, useState} from 'react';

export const useConvertN8nToWorkflow = () => {
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentComponent = useWorkflowNodeDetailsPanelStore((state) => state.currentComponent);
    const setWorkflow = useWorkflowDataStore((state) => state.setWorkflow);
    const [isRunning, setIsRunning] = useState(false);

    const convertN8nWorkflow = useCallback(
        async (workflowJson: string) => {
            const json = JSON.parse(workflowJson);

            const messageContent = JSON.stringify(json, null, 2);

            const agent = new HttpAgent({
                agentId: 'CONVERTER',
                headers: {'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''},
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
                environmentId: String(environmentStore.getState().currentEnvironmentId ?? 0),
                workflowId: workflow.id,
            });

            let convertedWorkflowBuffer = '';
            setIsRunning(true);

            const subscriber: AgentSubscriber = {
                onTextMessageContentEvent: ({event}) => {
                    convertedWorkflowBuffer += event.delta ?? '';
                },
                onTextMessageEndEvent: () => {
                    try {
                        const parsedWorkflow = JSON.parse(convertedWorkflowBuffer);

                        setWorkflow(parsedWorkflow);
                    } catch (e) {
                        console.error('Failed to parse workflow', e);

                        throw new Error('The n8n workflow could not be converted into a valid ByteChef workflow.', {
                            cause: e,
                        });
                    } finally {
                        setIsRunning(false);
                    }
                },
            };

            await agent.runAgent({runId: getRandomId()}, subscriber);

            return convertedWorkflowBuffer;
        },
        [workflow.id, currentComponent?.name, setWorkflow]
    );

    return {convertN8nWorkflow, isRunning};
};
