import PropertyCodeEditorSheetRightPanelConnections from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanelConnections';
import PropertyCodeEditorSheetRightPanelInputs from '@/pages/platform/workflow-editor/components/properties/components/property-code-editor/PropertyCodeEditorSheetRightPanelInputs';
import {ComponentConnection, Workflow} from '@/shared/middleware/platform/configuration';

import {getTask} from '../../../../utils/taskDispatcherConfig';

interface PropertyCodeEditorSheetConnectionsSheetRightPanelProps {
    componentConnections: ComponentConnection[];
    workflow: Workflow;
    workflowNodeName: string;
}

const PropertyCodeEditorSheetRightPanel = ({
    componentConnections,
    workflow,
    workflowNodeName,
}: PropertyCodeEditorSheetConnectionsSheetRightPanelProps) => {
    const currentTask = getTask({
        taskDispatcherId: workflowNodeName,
        tasks: workflow.tasks || [],
    });

    return (
        <div className="flex w-96 flex-col divide-y divide-solid divide-muted">
            <div className="flex-1">
                <PropertyCodeEditorSheetRightPanelInputs input={currentTask?.parameters?.input ?? {}} />
            </div>

            <div className="flex-1">
                <PropertyCodeEditorSheetRightPanelConnections
                    componentConnections={componentConnections}
                    workflow={workflow}
                    workflowNodeName={workflowNodeName}
                />
            </div>
        </div>
    );
};

export default PropertyCodeEditorSheetRightPanel;
