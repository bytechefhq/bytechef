import ConnectedUserSheetPanelIntegration from '@/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';

import type {ConnectedUserIntegrationInstance} from '@/shared/middleware/embedded/connected-user';

const ConnectedUserSheetPanelIntegrations = ({
    connectedUserId,
    connectedUserIntegrationInstances,
}: {
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
    connectedUserId: number;
    componentDefinitions?: ComponentDefinitionBasic[];
}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    return connectedUserIntegrationInstances.length > 0 ? (
        <div className="divide-y">
            {connectedUserIntegrationInstances.map((connectedUserIntegrationInstance) => {
                const componentDefinition = componentDefinitions?.find(
                    (componentDefinition) => componentDefinition.name === connectedUserIntegrationInstance.componentName
                );

                return (
                    componentDefinition &&
                    componentDefinitions && (
                        <ConnectedUserSheetPanelIntegration
                            componentDefinition={componentDefinition}
                            componentDefinitions={componentDefinitions}
                            connectedUserId={connectedUserId}
                            connectedUserIntegrationInstance={connectedUserIntegrationInstance}
                            key={connectedUserIntegrationInstance.id}
                        />
                    )
                );
            })}
        </div>
    ) : (
        <div className="py-4 text-sm">No active integrations.</div>
    );
};

export default ConnectedUserSheetPanelIntegrations;
