import Button from '@/components/Button/Button';
import RequiredMark from '@/components/RequiredMark';
import {Label} from '@/components/ui/label';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

export interface PropertyCodeEditorSheetRightPanelConnectionsLabelProps {
    onRemoveClick: () => void;
    componentConnection: ComponentConnection;
}
const PropertyCodeEditorSheetRightPanelConnectionsLabel = ({
    componentConnection,
    onRemoveClick,
}: PropertyCodeEditorSheetRightPanelConnectionsLabelProps) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    return (
        <div className="flex items-center justify-between">
            <div className="space-x-1">
                {componentDefinition && (
                    <Label>
                        <span>{componentDefinition?.title}</span>

                        {componentConnection.required && <RequiredMark />}
                    </Label>
                )}

                <Tooltip>
                    <TooltipTrigger>
                        <Label className="text-sm text-muted-foreground">{componentConnection.key}</Label>
                    </TooltipTrigger>

                    <TooltipContent>Workflow Connection Key</TooltipContent>
                </Tooltip>
            </div>

            <Button
                className="px-0 text-content-destructive-primary hover:text-content-destructive-primary active:text-content-destructive-primary"
                label="Remove"
                onClick={onRemoveClick}
                size="sm"
                variant="link"
            />
        </div>
    );
};

export default PropertyCodeEditorSheetRightPanelConnectionsLabel;
