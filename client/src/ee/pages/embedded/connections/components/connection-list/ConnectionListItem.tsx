import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import {DropdownMenuItem} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useOpenInEmbeddedEditor from '@/ee/pages/embedded/shared/useOpenInEmbeddedEditor';
import {Connection, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {useUpdateConnectionTagsMutation} from '@/ee/shared/mutations/embedded/connectionTags.mutations';
import {
    useDeleteConnectionMutation,
    useUpdateConnectionCredentialsMutation,
    useUpdateConnectionMutation,
} from '@/ee/shared/mutations/embedded/connections.mutations';
import {ConnectionKeys, useGetConnectionTagsQuery} from '@/ee/shared/queries/embedded/connections.queries';
import TagList from '@/shared/components/TagList';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import ConnectionListItemLayout from '@/shared/components/connection/ConnectionListItemLayout';
import ConnectionWorkflowList, {type ConnectionWorkflowI} from '@/shared/components/connection/ConnectionWorkflowList';
import {
    IntegrationConnectionWorkflowParentType,
    useIntegrationConnectionWorkflowsQuery,
} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionKeys,
    useGetConnectionComponentDefinitionQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {Trash2Icon} from 'lucide-react';
import {ReactNode, useMemo, useState} from 'react';

interface ConnectionListItemProps {
    badges?: ReactNode;
    componentDefinitions: ComponentDefinitionBasic[];
    connection: Connection;
    remainingTags?: Tag[];
}

const ConnectionListItem = ({badges, componentDefinitions, connection, remainingTags}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [workflowsOpen, setWorkflowsOpen] = useState(false);

    const {data: componentDefinition} = useGetConnectionComponentDefinitionQuery({
        componentName: connection.componentName,
        connectionVersion: connection.connectionVersion,
    });

    const {data: connectionWorkflowsData, isLoading: connectionWorkflowsLoading} =
        useIntegrationConnectionWorkflowsQuery(
            {connectionId: String(connection.id)},
            {enabled: workflowsOpen && connection.id !== undefined}
        );

    const queryClient = useQueryClient();
    const {openIntegrationWorkflow} = useOpenInEmbeddedEditor();

    const deleteConnectionMutation = useDeleteConnectionMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ComponentDefinitionKeys.componentDefinitions,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });
        },
    });

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connections,
            });
            void queryClient.invalidateQueries({
                queryKey: ConnectionKeys.connectionTags,
            });
        },
    });

    const connectionWorkflows = useMemo<ConnectionWorkflowI[] | undefined>(
        () =>
            connectionWorkflowsData?.integrationConnectionWorkflows.map((connectionWorkflow) => {
                const parentConnectedUser =
                    connectionWorkflow.parentType === IntegrationConnectionWorkflowParentType.ConnectedUser;

                const workflowUuid = connectionWorkflow.workflowUuid;

                return {
                    id: connectionWorkflow.id,
                    onOpenClick:
                        !parentConnectedUser && workflowUuid
                            ? () => void openIntegrationWorkflow(Number(connectionWorkflow.parentId), workflowUuid)
                            : undefined,
                    openLabel: 'Open in integration',
                    parentConnectedUser,
                    parentName: connectionWorkflow.parentName,
                    usage: connectionWorkflow.usage,
                    version: parentConnectedUser ? undefined : connectionWorkflow.parentVersion,
                    workflowName: connectionWorkflow.workflowName,
                };
            }),
        [connectionWorkflowsData, openIntegrationWorkflow]
    );

    const handleAlertDeleteDialogClick = () => {
        if (connection.id) {
            deleteConnectionMutation.mutate(connection.id);

            setShowDeleteDialog(false);
        }
    };

    if (!componentDefinition) {
        return <></>;
    }

    return (
        <ConnectionListItemLayout
            active={connection.active}
            badges={
                badges ??
                (connection.shared && (
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Badge
                                className="uppercase"
                                label="Shared"
                                styleType="secondary-outline"
                                weight="semibold"
                            />
                        </TooltipTrigger>

                        <TooltipContent>
                            Every connected user in this environment can use this connection
                        </TooltipContent>
                    </Tooltip>
                ))
            }
            createdDate={connection.createdDate}
            credentialStatus={connection.credentialStatus}
            expandLabel="Workflows"
            expandableContent={
                <ConnectionWorkflowList
                    connectionWorkflows={connectionWorkflows}
                    isLoading={connectionWorkflowsLoading}
                />
            }
            icon={componentDefinition.icon}
            id={connection.id}
            menuItems={
                <DropdownMenuItem
                    icon={<Trash2Icon />}
                    label="Delete"
                    onClick={() => setShowDeleteDialog(true)}
                    variant="destructive"
                />
            }
            name={connection.name}
            onEditClick={() => setShowEditDialog(true)}
            onOpenChange={setWorkflowsOpen}
            open={workflowsOpen}
            tagList={
                connection.tags && (
                    <TagList
                        getRequest={(id, tags) => ({
                            id: id!,
                            updateTagsRequest: {
                                tags,
                            },
                        })}
                        id={connection.id!}
                        remainingTags={remainingTags}
                        tags={connection.tags}
                        updateTagsMutation={updateConnectionTagsMutation}
                    />
                )
            }
        >
            <AlertDialog
                description="This action cannot be undone. This will permanently delete the connection."
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={handleAlertDeleteDialogClick}
                open={showDeleteDialog}
            />

            {showEditDialog && componentDefinitions && (
                <ConnectionDialog
                    componentDefinitions={componentDefinitions}
                    connection={connection}
                    connectionTagsQueryKey={ConnectionKeys.connectionTags}
                    connectionsQueryKey={ConnectionKeys.connections}
                    onClose={() => setShowEditDialog(false)}
                    showSharedOption
                    useGetConnectionTagsQuery={useGetConnectionTagsQuery}
                    useUpdateConnectionCredentialsMutation={useUpdateConnectionCredentialsMutation}
                    useUpdateConnectionMutation={useUpdateConnectionMutation}
                />
            )}
        </ConnectionListItemLayout>
    );
};

export default ConnectionListItem;
