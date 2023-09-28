import {ReactFlowProvider} from 'reactflow';

import NodeDetailsDialog from './components/NodeDetailsDialog';

import 'reactflow/dist/base.css';

import './Workflow.css';

import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';

import DataPillPanel from './components/DataPillPanel';
import WorkflowEditor, {WorkflowEditorProps} from './components/WorkflowEditor';

const Workflow = ({components, flowControls}: WorkflowEditorProps) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    return (
        <ReactFlowProvider>
            <WorkflowEditor
                components={components}
                flowControls={flowControls}
            />

            {componentDefinitions && (
                <NodeDetailsDialog
                    componentDefinitions={componentDefinitions}
                />
            )}

            <DataPillPanel />
        </ReactFlowProvider>
    );
};

export default Workflow;
