import {ReactFlowProvider} from 'reactflow';

import NodeDetailsDialog from './components/NodeDetailsDialog';

import 'reactflow/dist/base.css';

import './Workflow.css';

import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';

import DataPillPanel from './components/DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './components/WorkflowEditor';
import {useNodeDetailsDialogStore} from './stores/useNodeDetailsDialogStore';

const ProjectWorkflow = ({components, flowControls}: WorkflowEditorProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const {currentNode} = useNodeDetailsDialogStore();

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                components={components}
                flowControls={flowControls}
            />

            {componentDefinitions && currentNode.name && (
                <NodeDetailsDialog
                    componentDefinitions={componentDefinitions}
                />
            )}

            <DataPillPanel />
        </ReactFlowProvider>
    );
};

export default ProjectWorkflow;
