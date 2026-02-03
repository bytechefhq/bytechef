import PropertyCodeEditorDialogRightPanelConnections from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelConnections';
import PropertyCodeEditorDialogRightPanelInputs from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/property-code-editor-dialog/PropertyCodeEditorDialogRightPanelInputs';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';

interface PropertyCodeEditorDialogConnectionsSheetRightPanelProps {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorDialogRightPanel = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: PropertyCodeEditorDialogConnectionsSheetRightPanelProps) => {
    const currentTask = getTask({
        tasks: workflow.tasks || [],
        workflowNodeName,
    });

    return (
        <div className="flex w-96 flex-col divide-y divide-solid divide-muted border-l border-l-border/50">
            <div className="flex-1">
                <PropertyCodeEditorDialogRightPanelInputs input={currentTask?.parameters?.input ?? {}} />
            </div>

            <div className="flex-1">
                <PropertyCodeEditorDialogRightPanelConnections
                    componentConnections={componentConnections}
                    workflow={workflow}
                    workflowNodeName={workflowNodeName}
                />
            </div>
        </div>
    );
};

export default PropertyCodeEditorDialogRightPanel;
