import Button from '@/components/Button/Button';
import ConnectedUserSheetEmptyState from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetEmptyState';
import ConnectedUserSheetPanelIntegrationListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem';
import ProvisionIntegrationDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialog';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetProvisionableIntegrationsQuery} from '@/ee/shared/queries/embedded/provisionableIntegrations.queries';
import {PlusIcon, SquareIcon} from 'lucide-react';
import {useState} from 'react';

import type {ConnectedUser, ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';

const ConnectedUserSheetPanelIntegrationList = ({
    connectedUser,
    connectedUserId,
    connectedUserIntegrationInstances,
}: {
    connectedUser: Pick<ConnectedUser, 'email' | 'externalId' | 'name'>;
    connectedUserIntegrationInstances: ConnectedUserIntegrationInstance[];
    connectedUserId: number;
}) => {
    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({
        connectionDefinitions: true,
    });

    const [showProvisionDialog, setShowProvisionDialog] = useState(false);

    const {data: provisionableIntegrations} = useGetProvisionableIntegrationsQuery(connectedUserId);

    return (
        <div>
            {connectedUserIntegrationInstances.length > 0 ? (
                <>
                    <div className="flex items-center justify-end pb-2">
                        <Button
                            icon={<PlusIcon />}
                            label="Provision Integration"
                            onClick={() => setShowProvisionDialog(true)}
                            size="sm"
                            variant="secondary"
                        />
                    </div>

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
                                        connectedUser={connectedUser}
                                        connectedUserId={connectedUserId}
                                        connectedUserIntegrationInstance={connectedUserIntegrationInstance}
                                        key={connectedUserIntegrationInstance.id}
                                    />
                                )
                            );
                        })}
                    </div>
                </>
            ) : (
                <ConnectedUserSheetEmptyState
                    description="Provision an integration for this user and connect their account, so your product can run its actions on their behalf."
                    icon={<SquareIcon />}
                    nothingToProvision={provisionableIntegrations?.length === 0}
                    nothingToProvisionDescription="No integration can be provisioned yet. Publish an integration and enable a configuration for it in this environment first."
                    onProvision={() => setShowProvisionDialog(true)}
                    pageLink={{label: 'Go to Integrations', to: '/embedded/integrations'}}
                    provisionLabel="Provision Integration"
                    title="No integrations yet"
                />
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
