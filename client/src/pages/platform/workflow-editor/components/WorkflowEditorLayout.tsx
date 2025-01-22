import {ReactFlowProvider} from '@xyflow/react';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import '@xyflow/react/dist/base.css';

import './WorkflowEditorLayout.css';

import WorkflowTestChatPanel from '@/pages/platform/workflow-editor/components/workflow-test-chat/WorkflowTestChatPanel';
import {useWorkflowMutation} from '@/pages/platform/workflow-editor/providers/workflowMutationProvider';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowNodeParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {useEffect, useState} from 'react';

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
    const [currentNodeName, setCurrentNodeName] = useState<string | undefined>();

    const {componentActions, workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode, setCurrentComponent, setCurrentNode} = useWorkflowNodeDetailsPanelStore();

    const {updateWorkflowMutation} = useWorkflowMutation();

    const {data: workflowNodeParameterDisplayConditions} = useGetWorkflowNodeParameterDisplayConditionsQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentNodeName!,
        },
        !!currentNodeName && currentNodeName !== 'manual'
    );

    const {data: workflowNodeOutputs, refetch: refetchWorkflowNodeOutputs} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNodeName,
        },
        !!componentActions?.length && !!currentNodeName && !!currentNode && !currentNode?.trigger
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

    useEffect(() => {
        if (currentNode?.name) {
            setCurrentNodeName(currentNode?.name);
        } else {
            setCurrentNodeName(undefined);
        }
    }, [currentNode?.name]);

    // refetch workflowNodeOutputs when a task is opened
    useEffect(() => {
        if (!currentNode || currentNode?.trigger || workflowNodeOutputs || !workflow?.nodeNames) {
            return;
        }

        refetchWorkflowNodeOutputs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNodeName, workflow?.nodeNames.length]);

    // update display conditions when currentNode changes
    useEffect(() => {
        if (currentNode && workflowNodeParameterDisplayConditions?.displayConditions) {
            setCurrentNode({
                ...currentNode,
                displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
            });
        }

        if (currentComponent && workflowNodeParameterDisplayConditions?.displayConditions) {
            if (currentComponent.workflowNodeName === currentNode?.name) {
                setCurrentComponent({
                    ...currentComponent,
                    displayConditions: workflowNodeParameterDisplayConditions.displayConditions,
                });
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeParameterDisplayConditions?.displayConditions, currentNode?.name]);

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
