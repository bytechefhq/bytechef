import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './ProjectWorkflow.css';

import {useGetWorkflowNodeOutputsQuery} from '@/queries/platform/workflowNodeOutputs.queries';
import {useEffect} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import DataPillPanel from './DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './WorkflowEditor';

const ProjectWorkflow = ({
    componentDefinitions,
    projectId,
    taskDispatcherDefinitions,
    updateWorkflowMutation,
}: WorkflowEditorProps) => {
    const {componentActions, setComponentData, workflow} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: workflowNodeOutputs} = useGetWorkflowNodeOutputsQuery(
        {
            id: workflow.id!,
            lastWorkflowNodeName: currentNode.name,
        },
        !!componentActions?.length
    );

    useEffect(() => {
        const workflowComponentData = workflow.tasks?.map((task) => {
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

        if (workflowComponentData) {
            setComponentData(workflowComponentData);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks]);

    const previousComponentDefinitions = workflowNodeOutputs
        ? workflowNodeOutputs.map(
              (workflowStepOutput) =>
                  componentDefinitions.filter(
                      (componentDefinition) =>
                          componentDefinition.name === workflowStepOutput?.actionDefinition?.componentName
                  )[0]
          )
        : [];

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
                    previousComponentDefinitions={previousComponentDefinitions}
                    workflowStepOutputs={workflowNodeOutputs ?? []}
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

export default ProjectWorkflow;
