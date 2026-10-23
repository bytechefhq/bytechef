import Button from '@/components/Button/Button';
import ConnectedUserSheetPanelIntegrationListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem';
import ProvisionIntegrationDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialog';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useState} from 'react';

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

    const [showProvisionDialog, setShowProvisionDialog] = useState(false);

    return (
        <div>
            <div className="flex items-center justify-between pb-2">
                <h3 className="text-sm font-semibold">Integrations</h3>

                <Button
                    label="Add integration"
                    onClick={() => setShowProvisionDialog(true)}
                    size="sm"
                    variant="outline"
                />
            </div>

            {connectedUserIntegrationInstances.length > 0 ? (
                <div>
                    {connectedUserIntegrationInstances.map((connectedUserIntegrationInstance) => {
                        const componentDefinition = componentDefinitions?.find(
                            (componentDefinition) =>
                                componentDefinition.name === connectedUserIntegrationInstance.componentName
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
            )}

            {showProvisionDialog && (
                <ProvisionIntegrationDialog
                    connectedUserId={connectedUserId}
                    onClose={() => setShowProvisionDialog(false)}
                />
            )}
        </div>
    );
};

export default ConnectedUserSheetPanelIntegrationList;
