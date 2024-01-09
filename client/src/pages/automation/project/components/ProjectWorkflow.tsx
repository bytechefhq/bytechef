import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './ProjectWorkflow.css';

import {ActionDefinitionModel} from '@/middleware/hermes/configuration';
import {useGetActionDefinitionsQuery} from '@/queries/actionDefinitions.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {useEffect, useState} from 'react';

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
    const [actionData, setActionData] = useState<Array<ActionDefinitionModel>>([]);

    const {componentActions, componentNames, nodeNames} = useWorkflowDataStore();
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    const currentNodeIndex = nodeNames.indexOf(currentNode.name);

    const previousComponentNames = componentNames.length > 1 ? componentNames.slice(0, currentNodeIndex) : [];

    const normalizedPreviousComponentNames = previousComponentNames.map((name) =>
        name.match(new RegExp(/_\d$/)) ? name.slice(0, name.length - 2) : name
    );

    const {data: connectionComponentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {data: previousComponentDefinitions} = useGetComponentDefinitionsQuery(
        {
            include: normalizedPreviousComponentNames,
        },
        !!normalizedPreviousComponentNames.length
    );

    const taskTypes = componentActions?.map(
        (componentAction) => `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const {data: actionDefinitions} = useGetActionDefinitionsQuery({taskTypes}, !!previousComponentDefinitions?.length);

    useEffect(() => {
        if (actionDefinitions) {
            setActionData(actionDefinitions);
        }
    }, [actionDefinitions]);

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                projectId={+projectId!}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
                workflowId={workflowId}
            />

            {connectionComponentDefinitions && currentNode.name && (
                <WorkflowNodeDetailsPanel
                    componentDefinitions={connectionComponentDefinitions}
                    workflowId={workflowId}
                />
            )}

            {previousComponentDefinitions && (
                <DataPillPanel
                    actionData={actionData}
                    normalizedPreviousComponentNames={normalizedPreviousComponentNames}
                    previousComponentDefinitions={previousComponentDefinitions}
                    previousComponentNames={previousComponentNames}
                />
            )}
        </ReactFlowProvider>
    );
};

export default ProjectWorkflow;
