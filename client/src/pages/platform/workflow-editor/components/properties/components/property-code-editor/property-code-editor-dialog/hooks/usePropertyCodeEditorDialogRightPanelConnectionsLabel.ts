import {ComponentConnection} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';

interface UsePropertyCodeEditorDialogRightPanelConnectionsLabelProps {
    componentConnection: ComponentConnection;
}

const usePropertyCodeEditorDialogRightPanelConnectionsLabel = ({
    componentConnection,
}: UsePropertyCodeEditorDialogRightPanelConnectionsLabelProps) => {
    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: componentConnection.componentName,
        componentVersion: componentConnection.componentVersion,
    });

    return {
        componentDefinition,
    };
};

export default usePropertyCodeEditorDialogRightPanelConnectionsLabel;
