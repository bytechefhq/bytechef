import RequiredMark from '@/components/RequiredMark';
import {Button} from '@/components/ui/button';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {WorkflowConnection} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

export interface PropertyCodeEditorSheetRightPanelConnectionsLabelProps {
    onRemoveClick: () => void;
    workflowConnection: WorkflowConnection;
}
const PropertyCodeEditorSheetRightPanelConnectionsLabel = ({
    onRemoveClick,
    workflowConnection,
}: PropertyCodeEditorSheetRightPanelConnectionsLabelProps) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    return (
        <div className="flex items-center justify-between">
            <div className="space-x-1">
                {componentDefinition && (
                    <Label>
                        <span>{componentDefinition?.title}</span>

                        {workflowConnection.required && <RequiredMark />}
                    </Label>
                )}

                <Tooltip>
                    <TooltipTrigger>
                        <Label className="text-sm text-muted-foreground">{workflowConnection.key}</Label>
                    </TooltipTrigger>

                    <TooltipContent>Workflow Connection Key</TooltipContent>
                </Tooltip>
            </div>

            <Button className="px-0 text-destructive" onClick={onRemoveClick} size="sm" variant="link">
                Remove
            </Button>
        </div>
    );
};

export default PropertyCodeEditorSheetRightPanelConnectionsLabel;
