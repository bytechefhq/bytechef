import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './WorkflowEditorLayout.css';

import {ComponentDefinitionBasicModel, TaskDispatcherDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useGetWorkflowNodeOutputsQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {ComponentType, UpdateWorkflowMutationType} from '@/types/types';
import {useEffect, useState} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
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
    const {currentNode, setCurrentComponent, setWorkflowNodeDetailsPanelOpen, workflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore();

    const {data: workflowNodeOutputs, refetch: refetchWorkflowNodeOutputs} = useGetWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNodeName,
        },
        !!componentActions?.length && !!currentNodeName && !!currentNode && !currentNode?.trigger
    );

    const previousComponentDefinitions = workflowNodeOutputs
        ? workflowNodeOutputs
              .filter(
                  (workflowNodeOutput) => workflowNodeOutput.actionDefinition || workflowNodeOutput.triggerDefinition
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

    useEffect(() => {
        if (!workflowNodeDetailsPanelOpen && currentNodeName) {
            setWorkflowNodeDetailsPanelOpen(true);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [currentNodeName, workflowNodeDetailsPanelOpen]);

    // refetch workflowNodeOutputs when a new node is added
    useEffect(() => {
        if (currentNode && !currentNode?.trigger) {
            refetchWorkflowNodeOutputs();
        }
    }, [workflow.tasks?.length, currentNode, refetchWorkflowNodeOutputs]);

    useEffect(() => {
        const workflowComponents = [...(workflow.triggers ?? []), ...(workflow.tasks ?? [])]?.map((operation) => {
            const {description, label, name, parameters, type} = operation;

            const [componentName, operationName] = type.split('/v1/');

            return {
                componentName,
                notes: description,
                operationName,
                parameters,
                title: label,
                type,
                workflowNodeName: name,
            } as ComponentType;
        });

        if (workflowComponents && currentNodeName) {
            setCurrentComponent(workflowComponents.find((component) => component.workflowNodeName === currentNodeName));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks, workflow.triggers, currentNodeName]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                updateWorkflowMutation={updateWorkflowMutation}
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
