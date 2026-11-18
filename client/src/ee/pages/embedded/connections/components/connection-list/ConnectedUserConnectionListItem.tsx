import Badge from '@/components/Badge/Badge';
import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import ConnectedUserConnectionDialogs from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionDialogs';
import {getConnectedUserConnectionPermissions} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectedUserConnectionPermissions';
import ConnectionOriginBadge from '@/ee/pages/embedded/connections/components/connection-list/ConnectionOriginBadge';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {Trash2Icon} from 'lucide-react';
import {useState} from 'react';

interface ConnectedUserConnectionListItemProps {
    componentDefinitions: ComponentDefinitionBasic[];
    connectedUserId: number;
    connection: Connection;
    provisionedIntegrationInstance?: ConnectedUserIntegrationInstance;
}

const ConnectedUserConnectionListItem = ({
    componentDefinitions,
    connectedUserId,
    connection,
    provisionedIntegrationInstance,
}: ConnectedUserConnectionListItemProps) => {
    const [showCredentialsDialog, setShowCredentialsDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {credentialsEditable, deletable} = getConnectedUserConnectionPermissions(
        connection,
        provisionedIntegrationInstance
    );

    const provisionedDeletable = deletable && connection.origin === ConnectionOriginEnum.Provisioned;

    const componentDefinition = componentDefinitions.find(
        (componentDefinition) => componentDefinition.name === connection.componentName
    );

    return (
        <ConnectionListItemLayout
            active={connection.active}
            badges={<ConnectionOriginBadge origin={connection.origin} />}
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            icon={componentDefinition?.icon}
            id={connection.id}
            menuItems={
                provisionedDeletable && (
                    <DropdownMenuItem
                        icon={<Trash2Icon />}
                        label="Delete"
                        onClick={() => setShowDeleteDialog(true)}
                        variant="destructive"
                    />
                )
            }
            name={connection.name}
            onEditClick={credentialsEditable ? () => setShowCredentialsDialog(true) : undefined}
            tagList={
                !!connection.tags?.length && (
                    <div className="flex flex-wrap items-center gap-1">
                        {connection.tags?.map((tag) => (
                            <Badge key={tag.id} label={tag.name} styleType="secondary-outline" weight="semibold" />
                        ))}
                    </div>
                )
            }
        >
            <ConnectedUserConnectionDialogs
                connectedUserId={connectedUserId}
                connection={connection}
                deletable={provisionedDeletable}
                onCredentialsDialogClose={() => setShowCredentialsDialog(false)}
                onDeleteDialogClose={() => setShowDeleteDialog(false)}
                provisionedIntegrationInstance={provisionedIntegrationInstance}
                showCredentialsDialog={showCredentialsDialog}
                showDeleteDialog={showDeleteDialog}
            />
        </ConnectionListItemLayout>
    );
};

export default ConnectedUserConnectionListItem;
