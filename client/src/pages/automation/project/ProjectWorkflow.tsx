import {ReactFlowProvider} from 'reactflow';

import WorkflowNodeDetailsPanel from './components/WorkflowNodeDetailsPanel';

import 'reactflow/dist/base.css';

import './Workflow.css';

import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';

import DataPillPanel from './components/DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './components/WorkflowEditor';
import {useNodeDetailsPanelStore} from './stores/useNodeDetailsPanelStore';

const ProjectWorkflow = ({
    componentDefinitions,
    taskDispatcherDefinitions,
}: WorkflowEditorProps) => {
    const {data: connectionComponentDefinitions} =
        useGetComponentDefinitionsQuery({
            connectionDefinitions: true,
        });

    const {currentNode} = useNodeDetailsPanelStore();

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                componentDefinitions={componentDefinitions}
                taskDispatcherDefinitions={taskDispatcherDefinitions}
            />

            {connectionComponentDefinitions && currentNode.name && (
                <WorkflowNodeDetailsPanel
                    componentDefinitions={connectionComponentDefinitions}
                />
            )}

            <DataPillPanel />
        </ReactFlowProvider>
    );
};

export default ProjectWorkflow;
