import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './WorkflowEditorLayout.css';

import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from '@/shared/middleware/platform/configuration';
import {useGetPreviousWorkflowNodeOutputsQuery} from '@/shared/queries/platform/workflowNodeOutputs.queries';
import {useGetWorkflowNodeParameterDisplayConditionsQuery} from '@/shared/queries/platform/workflowNodeParameters.queries';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {useEffect, useState} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import DataPillPanel from './DataPillPanel';
import WorkflowEditor from './WorkflowEditor';

export interface WorkflowEditorLayoutProps {
    componentDefinitions: ComponentDefinitionBasicModel[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

const WorkflowEditorLayout = ({
    componentDefinitions,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorLayoutProps) => {
    const [currentNodeName, setCurrentNodeName] = useState<string | undefined>();

    const {componentActions, workflow} = useWorkflowDataStore();
    const {currentComponent, currentNode, setCurrentComponent} = useWorkflowNodeDetailsPanelStore();

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: workflowNodeParameterDisplayConditions} = useGetWorkflowNodeParameterDisplayConditionsQuery(
        {
            id: workflow.id!,
            workflowNodeName: currentComponent?.workflowNodeName!,
        },
        !!currentComponent && !!currentComponent?.workflowNodeName && currentComponent?.componentName !== 'manual'
    );

    const {data: workflowNodeOutputs, refetch: refetchWorkflowNodeOutputs} = useGetPreviousWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNodeName,
        },
        !!componentActions?.length && !!currentNodeName && !!currentNode && !currentNode?.trigger
    );

    const previousComponentDefinitions =
        !currentNode?.trigger && workflowNodeOutputs
            ? workflowNodeOutputs
                  .filter(
                      (workflowNodeOutput) =>
                          workflowNodeOutput.actionDefinition || workflowNodeOutput.triggerDefinition
                  )
                  .map(
                      (workflowNodeOutput) =>
                          componentDefinitions.filter(
                              (componentDefinition) =>
                                  componentDefinition.name === workflowNodeOutput?.actionDefinition?.componentName ||
                                  componentDefinition.name === workflowNodeOutput?.triggerDefinition?.componentName
                          )[0]
                  )
                  .filter((componentDefinition) => !!componentDefinition)
            : [];

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
        if (currentComponent && workflowNodeParameterDisplayConditions?.displayConditions) {
            setCurrentComponent({
                ...currentComponent,
                displayConditions: workflowNodeParameterDisplayConditions?.displayConditions!,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeParameterDisplayConditions?.displayConditions, currentNode?.name]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
            />

            {currentComponent && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    updateWorkflowMutation={updateWorkflowMutation}
                    workflowNodeOutputs={workflowNodeOutputs ?? []}
                />
            )}

            {(workflowNodeOutputs || (!workflowNodeOutputs && currentNode?.trigger)) &&
                previousComponentDefinitions && (
                    <DataPillPanel
                        previousComponentDefinitions={previousComponentDefinitions}
                        workflowNodeOutputs={workflowNodeOutputs ?? []}
                    />
                )}
        </ReactFlowProvider>
    );
};

export default WorkflowEditorLayout;
