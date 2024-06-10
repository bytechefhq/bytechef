import {Label} from '@/components/ui/label';
import {WorkflowConnectionModel} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

const ConnectionTabConnectionLabel = ({
    workflowConnection,
    workflowConnectionsCount,
}: {
    workflowConnection: WorkflowConnectionModel;
    workflowConnectionsCount: number;
}) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: workflowConnection.componentName,
        componentVersion: workflowConnection.componentVersion,
    });

    return (
        <div className="space-x-1">
            {componentDefinition && (
                <Label>
                    {`${componentDefinition.title}`}

                    {workflowConnection.required && <span className="ml-0.5 leading-3 text-red-500">*</span>}
                </Label>
            )}

            {workflowConnectionsCount > 1 && (
                <Label className="text-sm text-muted-foreground">{workflowConnection.key}</Label>
            )}
        </div>
    );
};

export default ConnectionTabConnectionLabel;
