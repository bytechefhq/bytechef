import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './ProjectWorkflow.css';

import {useGetWorkflowNodeOutputsQuery} from '@/queries/platform/workflowNodeOutputs.queries';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {useWorkflowNodeDetailsPanelStore} from '../stores/useWorkflowNodeDetailsPanelStore';
import DataPillPanel from './DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './WorkflowEditor';

const ProjectWorkflow = ({
    componentDefinitions,
    projectId,
    taskDispatcherDefinitions,
    workflowId,
}: WorkflowEditorProps) => {
    const {componentActions} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const {data: workflowNodeOutputs} = useGetWorkflowNodeOutputsQuery(
        {
            id: workflowId,
            lastWorkflowNodeName: currentNode.name,
        },
        !!componentActions?.length
    );

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
                workflowId={workflowId}
            />

            {currentNode.name && (
                <WorkflowNodeDetailsPanel
                    previousComponentDefinitions={previousComponentDefinitions}
                    workflowId={workflowId}
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
