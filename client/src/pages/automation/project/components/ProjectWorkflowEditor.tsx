import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './ProjectWorkflow.css';

import {ProjectKeys} from '@/queries/automation/projects.queries';
import {useGetWorkflowNodeOutputsQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import DataPillPanel from './DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './WorkflowEditor';

const ProjectWorkflowEditor = ({
    componentDefinitions,
    projectId,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorProps) => {
    const {componentActions, setComponents, workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: workflowNodeOutputs, refetch: refetchWorkflowNodeOutputs} = useGetWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNode.name,
        },
        !!componentActions?.length
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
        : [];

    const queryClient = useQueryClient();

    const handlePropertiesChange = () => {
        queryClient.invalidateQueries({
            queryKey: ProjectKeys.project(+projectId),
        });
    };

    // refetch workflowNodeOutputs when a new node is added
    useEffect(() => {
        refetchWorkflowNodeOutputs();
    }, [workflow.tasks?.length, refetchWorkflowNodeOutputs]);

    useEffect(() => {
        const workflowComponents = workflow.tasks?.map((task) => {
            const {label, name, parameters, type} = task;

            const [componentName, actionName] = type.split('/v1/');

            return {
                actionName,
                componentName,
                parameters,
                title: label,
                type,
                workflowNodeName: name,
            };
        });

        if (workflowComponents) {
            setComponents(workflowComponents);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                projectId={+projectId!}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                updateWorkflowMutation={updateWorkflowMutation}
            />

            {currentNode.name && (
                <WorkflowNodeDetailsPanel
                    onPropertyChange={handlePropertiesChange}
                    previousComponentDefinitions={previousComponentDefinitions}
                    workflowNodeOutputs={workflowNodeOutputs ?? []}
                />
            )}

            {workflowNodeOutputs && previousComponentDefinitions && (
                <DataPillPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    workflowNodeOutputs={workflowNodeOutputs ?? []}
                />
            )}
        </ReactFlowProvider>
    );
};

export default ProjectWorkflowEditor;
