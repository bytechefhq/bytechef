import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './ProjectWorkflow.css';

import {useGetActionDefinitionsQuery} from '@/queries/actionDefinitions.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';

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
    const {componentActions, componentNames, nodeNames} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const currentNodeIndex = nodeNames.indexOf(currentNode.name);

    const previousComponentNames = componentNames.length > 1 ? componentNames.slice(0, currentNodeIndex) : [];

    const normalizedPreviousComponentNames = previousComponentNames.map((name) =>
        name.match(new RegExp(/_\d$/)) ? name.slice(0, name.length - 2) : name
    );

    const {data: previousComponentDefinitions} = useGetComponentDefinitionsQuery(
        {
            include: normalizedPreviousComponentNames,
        },
        !!normalizedPreviousComponentNames.length
    );

    const taskTypes = componentActions?.map(
        (componentAction) => `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const {data: actionDefinitions} = useGetActionDefinitionsQuery({taskTypes}, !!componentActions?.length);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                projectId={+projectId!}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                workflowId={workflowId}
            />

            {actionDefinitions && currentNode.name && (
                <WorkflowNodeDetailsPanel
                    actionDefinitions={actionDefinitions}
                    previousComponentDefinitions={previousComponentDefinitions ?? []}
                />
            )}

            {actionDefinitions && previousComponentDefinitions && (
                <DataPillPanel
                    actionDefinitions={actionDefinitions}
                    normalizedPreviousComponentNames={normalizedPreviousComponentNames}
                    previousComponentDefinitions={previousComponentDefinitions}
                    previousComponentNames={previousComponentNames}
                />
            )}
        </ReactFlowProvider>
    );
};

export default ProjectWorkflow;
