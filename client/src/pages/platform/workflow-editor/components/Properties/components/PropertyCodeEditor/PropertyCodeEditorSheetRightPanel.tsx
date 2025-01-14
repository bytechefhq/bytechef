import PropertyCodeEditorSheetRightPanelConnections from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditorSheetRightPanelConnections';
import PropertyCodeEditorSheetRightPanelInputs from '@/pages/platform/workflow-editor/components/Properties/components/PropertyCodeEditor/PropertyCodeEditorSheetRightPanelInputs';
import {Workflow, WorkflowConnection} from '@/shared/middleware/platform/configuration';

interface PropertyCodeEditorSheetConnectionsSheetRightPanelProps {
    workflowConnections: WorkflowConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorSheetRightPanel = ({
    workflow,
    workflowConnections,
    workflowNodeName,
}: PropertyCodeEditorSheetConnectionsSheetRightPanelProps) => {
    return (
        <div className="flex w-96 flex-col divide-y divide-solid divide-muted">
            <div className="flex-1">
                <PropertyCodeEditorSheetRightPanelInputs
                    input={workflow.tasks?.find((task) => task.name === workflowNodeName)?.parameters?.input ?? {}}
                />
            </div>

            <div className="flex-1">
                <PropertyCodeEditorSheetRightPanelConnections
                    workflow={workflow}
                    workflowConnections={workflowConnections}
                    workflowNodeName={workflowNodeName}
                />
            </div>
        </div>
    );
};

export default PropertyCodeEditorSheetRightPanel;
