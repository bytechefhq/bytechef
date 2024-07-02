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
import {ComponentType, UpdateWorkflowMutationType} from '@/shared/types';
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
        !!currentComponent && !!currentComponent?.workflowNodeName
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
        if (!currentNode || currentNode?.trigger || workflowNodeOutputs) {
            return;
        }

        refetchWorkflowNodeOutputs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNodeName, workflowNodeOutputs]);

    // set currentComponent when currentNodeName changes
    useEffect(() => {
        const workflowComponents = [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])]?.map((workflowNode) => {
            const {description, label, metadata, name, parameters, type} = workflowNode;

            const [componentName, operationName] = type.split('/v1/');

            return {
                componentName,
                metadata,
                notes: description,
                operationName,
                parameters,
                title: label,
                type,
                workflowNodeName: name,
            } as ComponentType;
        });

        if (workflowComponents && currentNodeName && currentComponent?.workflowNodeName !== currentNodeName) {
            setCurrentComponent(workflowComponents.find((component) => component.workflowNodeName === currentNodeName));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks, workflow.triggers, currentNodeName]);

    useEffect(() => {
        if (currentComponent && workflowNodeParameterDisplayConditions?.displayConditions) {
            setCurrentComponent({
                ...currentComponent,
                displayConditions: workflowNodeParameterDisplayConditions?.displayConditions!,
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowNodeParameterDisplayConditions?.displayConditions, currentNode?.name]);

    useEffect(() => {
        refetchWorkflowNodeOutputs();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow?.nodeNames]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
            />

            {currentNodeName && (
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
