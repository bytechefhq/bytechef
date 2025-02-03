import {ReactFlowProvider} from '@xyflow/react';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useEffect} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import DataPillPanel from './DataPillPanel';
import WorkflowEditor from './WorkflowEditor';

export interface WorkflowEditorLayoutProps {
    componentDefinitions: ComponentDefinitionBasic[];
    leftSidebarOpen: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const WorkflowEditorLayout = ({
    componentDefinitions,
    leftSidebarOpen,
    taskDispatcherDefinitions,
}: WorkflowEditorLayoutProps) => {
    const {componentActions, workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode} = useWorkflowNodeDetailsPanelStore();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {data: workflowNodeOutputs, refetch: refetchWorkflowNodeOutputs} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNode?.name,
        },
        !!componentActions?.length && !!currentNode && !!currentNode?.name && !currentNode?.trigger
    );

    let previousComponentDefinitions: ComponentDefinitionBasic[] = [];

    let filteredWorkflowNodeOutputs;

    if (!currentNode?.trigger && workflowNodeOutputs) {
        filteredWorkflowNodeOutputs = workflowNodeOutputs.filter(
            (workflowNodeOutput) => workflowNodeOutput.actionDefinition || workflowNodeOutput.triggerDefinition
        );

        previousComponentDefinitions = filteredWorkflowNodeOutputs
            .map(
                (workflowNodeOutput) =>
                    componentDefinitions.filter(
                        (componentDefinition) =>
                            componentDefinition.name === workflowNodeOutput?.actionDefinition?.componentName ||
                            componentDefinition.name === workflowNodeOutput?.triggerDefinition?.componentName
                    )[0]
            )
            .filter((componentDefinition) => !!componentDefinition);
    }

    // refetch workflowNodeOutputs when a task is opened
    useEffect(() => {
        if (!currentNode || currentNode?.trigger || workflowNodeOutputs || !workflow?.nodeNames) {
            return;
        }

        refetchWorkflowNodeOutputs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNode?.name, workflow?.nodeNames.length]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                leftSidebarOpen={leftSidebarOpen}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
            />

            {currentComponent && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                />
            )}

            {workflow.id && <WorkflowTestChatPanel />}

            {(filteredWorkflowNodeOutputs || (!filteredWorkflowNodeOutputs && currentNode?.trigger)) &&
                previousComponentDefinitions && (
                    <DataPillPanel
                        previousComponentDefinitions={previousComponentDefinitions}
                        workflowNodeOutputs={filteredWorkflowNodeOutputs ?? []}
                    />
                )}
        </ReactFlowProvider>
    );
};

export default WorkflowEditorLayout;
