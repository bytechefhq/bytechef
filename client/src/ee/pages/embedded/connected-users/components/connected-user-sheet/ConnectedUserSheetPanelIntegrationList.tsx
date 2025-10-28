import ConnectedUserSheetPanelIntegrationListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

import type {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';

const ConnectedUserSheetPanelIntegrationList = ({
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
                        <ConnectedUserSheetPanelIntegrationListItem
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

export default ConnectedUserSheetPanelIntegrationList;
