import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserConnectionDialogs from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserConnectionDialogs';
import ConnectedUserCredentialStatus from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserCredentialStatus';
import ConnectedUserSheetDeleteDropdownMenu from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetDeleteDropdownMenu';
import {getConnectedUserConnectionPermissions} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectedUserConnectionPermissions';
import {CONNECTION_ORIGIN_BADGES} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connectionOriginBadges';
import {Connection, ConnectionOriginEnum} from '@/ee/shared/middleware/embedded/configuration';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {ComponentIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';

interface ConnectedUserSheetPanelConnectionListItemProps {
    componentTitle?: string;
    connectedUserId: number;
    connection: Connection;
    icon?: string;
    provisionedIntegrationInstance?: ConnectedUserIntegrationInstance;
    usedByIntegration?: boolean;
}

const ConnectedUserSheetPanelConnectionListItem = ({
    componentTitle,
    connectedUserId,
    connection,
    icon,
    provisionedIntegrationInstance,
    usedByIntegration = false,
}: ConnectedUserSheetPanelConnectionListItemProps) => {
    const [showCredentialsDialog, setShowCredentialsDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const originBadge = connection.origin && CONNECTION_ORIGIN_BADGES[connection.origin];

    const {credentialsEditable, deletable, shared} = getConnectedUserConnectionPermissions(
        connection,
        provisionedIntegrationInstance
    );

    const hasActions = credentialsEditable || shared || deletable;

    return (
        <li className="mb-2 flex items-center justify-between rounded border border-border/50 px-3 hover:bg-surface-neutral-primary-hover">
            <div className="flex flex-1 items-center gap-x-3 py-3">
                {icon ? (
                    <InlineSVG aria-hidden="true" className="size-6 shrink-0" src={icon} />
                ) : (
                    <ComponentIcon aria-hidden="true" className="size-6 shrink-0 text-content-neutral-secondary" />
                )}

                <div className="flex flex-col items-start justify-center gap-y-1">
                    <span className="flex min-h-6 items-center text-base font-semibold">{connection.name}</span>

                    <div className="flex min-h-6 items-center text-xs font-semibold text-muted-foreground">
                        <ConnectedUserCredentialStatus
                            componentTitle={componentTitle || connection.componentName}
                            credentialStatus={connection.credentialStatus}
                        />
                    </div>
                </div>
            </div>

            <div className="flex items-center gap-x-2">
                <div className="flex shrink-0 items-center justify-end">
                    <div className="flex flex-col items-end gap-y-1">
                        <div className="flex min-h-6 items-center gap-2">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Badge
                                        label={usedByIntegration ? 'Integration' : 'Automation'}
                                        styleType="secondary-filled"
                                        weight="semibold"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>
                                    {usedByIntegration
                                        ? `Used by this user's ${componentTitle || connection.componentName} integration`
                                        : connection.origin === ConnectionOriginEnum.Internal
                                          ? 'An admin connection automation configurations provide to their workflows'
                                          : 'Used by automation workflows'}
                                </TooltipContent>
                            </Tooltip>

                            {originBadge && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Badge
                                            className="uppercase"
                                            label={originBadge.label}
                                            styleType="secondary-outline"
                                            weight="semibold"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>{originBadge.description}</TooltipContent>
                                </Tooltip>
                            )}
                        </div>

                        {connection.createdDate && (
                            <Tooltip>
                                <TooltipTrigger className="flex min-h-6 items-center text-xs whitespace-nowrap text-content-neutral-secondary">
                                    {`Created at ${connection.createdDate.toLocaleDateString()} ${connection.createdDate.toLocaleTimeString()}`}
                                </TooltipTrigger>

                                <TooltipContent side="left">Created Date</TooltipContent>
                            </Tooltip>
                        )}
                    </div>
                </div>

                {hasActions ? (
                    <ConnectedUserSheetDeleteDropdownMenu
                        credentialsDisabledReason={
                            shared ? 'Shared connections are edited on the Connections page' : undefined
                        }
                        onCredentialsClick={credentialsEditable ? () => setShowCredentialsDialog(true) : undefined}
                        onDeleteClick={deletable ? () => setShowDeleteDialog(true) : undefined}
                    />
                ) : (
                    <div aria-hidden="true" className="size-9 shrink-0" />
                )}
            </div>

            <ConnectedUserConnectionDialogs
                connectedUserId={connectedUserId}
                connection={connection}
                deletable={deletable}
                onCredentialsDialogClose={() => setShowCredentialsDialog(false)}
                onDeleteDialogClose={() => setShowDeleteDialog(false)}
                provisionedIntegrationInstance={provisionedIntegrationInstance}
                showCredentialsDialog={showCredentialsDialog}
                showDeleteDialog={showDeleteDialog}
            />
        </li>
    );
};

export default ConnectedUserSheetPanelConnectionListItem;
