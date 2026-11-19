import Button from '@/components/Button/Button';
import ConnectedUserSheetEmptyState from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetEmptyState';
import ConnectedUserSheetPanelIntegrationListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem';
import ProvisionIntegrationDialog from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/provision-integration-dialog/ProvisionIntegrationDialog';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useGetProvisionableIntegrationsQuery} from '@/ee/shared/queries/embedded/provisionableIntegrations.queries';
import ListGroupHeader from '@/shared/components/ListGroupHeader';
import {ComponentIcon, PlusIcon, SquareIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

import type {ConnectedUser, ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import type {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

interface IntegrationGroupI {
    componentDefinition: ComponentDefinitionBasic;
    instances: ConnectedUserIntegrationInstance[];
    integrationId: number;
    integrationName: string;
}

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

    const {data: integrations} = useGetIntegrationsQuery();

    const integrationGroups = useMemo(() => {
        const groupsByIntegrationId = new Map<number, IntegrationGroupI>();

        for (const connectedUserIntegrationInstance of connectedUserIntegrationInstances) {
            const componentDefinition = componentDefinitions?.find(
                (componentDefinition) => componentDefinition.name === connectedUserIntegrationInstance.componentName
            );

            const integrationId = connectedUserIntegrationInstance.integrationId;

            if (!componentDefinition || integrationId == null) {
                continue;
            }

            const integration = integrations?.find((integration) => integration.id === integrationId);

            const group = groupsByIntegrationId.get(integrationId) ?? {
                componentDefinition,
                instances: [],
                integrationId,
                integrationName: integration?.name ?? componentDefinition.title ?? '',
            };

            group.instances.push(connectedUserIntegrationInstance);

            groupsByIntegrationId.set(integrationId, group);
        }

        return [...groupsByIntegrationId.values()].sort(
            (firstGroup, secondGroup) =>
                firstGroup.integrationName.localeCompare(secondGroup.integrationName) ||
                firstGroup.integrationId - secondGroup.integrationId
        );
    }, [componentDefinitions, connectedUserIntegrationInstances, integrations]);

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
                        {integrationGroups.map(
                            ({componentDefinition, instances, integrationId, integrationName}, groupIndex) => (
                                <section aria-label={integrationName} key={integrationId}>
                                    <ListGroupHeader
                                        className={groupIndex === 0 ? 'pt-0' : undefined}
                                        icon={
                                            componentDefinition.icon ? (
                                                <InlineSVG
                                                    aria-hidden="true"
                                                    className="size-5 shrink-0"
                                                    src={componentDefinition.icon}
                                                >
                                                    <ComponentIcon className="size-5 shrink-0" />
                                                </InlineSVG>
                                            ) : (
                                                <ComponentIcon aria-hidden="true" className="size-5 shrink-0" />
                                            )
                                        }
                                        name={integrationName}
                                    />

                                    {instances.map((connectedUserIntegrationInstance) => (
                                        <ConnectedUserSheetPanelIntegrationListItem
                                            componentDefinition={componentDefinition}
                                            connectedUser={connectedUser}
                                            connectedUserId={connectedUserId}
                                            connectedUserIntegrationInstance={connectedUserIntegrationInstance}
                                            key={connectedUserIntegrationInstance.id}
                                        />
                                    ))}
                                </section>
                            )
                        )}
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
