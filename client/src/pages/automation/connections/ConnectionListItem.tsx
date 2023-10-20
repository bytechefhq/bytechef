import {CalendarIcon} from '@heroicons/react/24/outline';
import {Component1Icon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import React, {useState} from 'react';

import AlertDialog from '../../../components/AlertDialog/AlertDialog';
import Badge from '../../../components/Badge/Badge';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import TagList from '../../../components/TagList/TagList';
import {ConnectionModel, TagModel} from '../../../middleware/connection';
import {
    useDeleteConnectionMutation,
    useUpdateConnectionTagsMutation,
} from '../../../mutations/connections.mutations';
import { ComponentDefinitionKeys, useGetComponentDefinitionQuery } from "../../../queries/componentDefinitions.queries";
import {ConnectionKeys} from '../../../queries/connections.queries';
import ConnectionDialog from './components/ConnectionDialog';
import InlineSVG from "react-inlinesvg";

interface ConnectionListItemProps {
    connection: ConnectionModel;
    remainingTags?: TagModel[];
}

const ConnectionListItem = ({
    connection,
    remainingTags,
}: ConnectionListItemProps) => {
    const [showEditDialog, setShowEditDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: connection.componentName,
    });

    const queryClient = useQueryClient();

    const deleteConnectionMutation = useDeleteConnectionMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(
                ComponentDefinitionKeys.componentDefinitions({
                    connectionInstances: true,
                })
            );
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);
        },
    });

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);
        },
    });

    const menuItems: IDropdownMenuItem[] = [
        {
            label: 'Edit',
            onClick: () => setShowEditDialog(true),
        },
        {
            separator: true,
        },
        {
            danger: true,
            label: 'Delete',
            onClick: () => setShowDeleteDialog(true),
        },
    ];

    return (
        <>
            <div className="flex items-center">
                <div className="flex-1 pr-8">
                    <div className="flex items-center justify-between">
                        <div className="relative flex items-center">
                            {componentDefinition?.icon && <InlineSVG className="mr-1 h-6 w-6 flex-none" src={componentDefinition.icon} />}

                            {!componentDefinition?.icon && <Component1Icon className="mr-1 h-6 w-6 flex-none" />}

                            <span className="mr-2 text-base font-semibold text-gray-900">
                                {connection.name}
                            </span>
                        </div>

                        <div className="ml-2 flex shrink-0">
                            <Badge
                                color={connection.active ? 'green' : 'default'}
                                text={
                                    connection.active ? 'Active' : 'Not Active'
                                }
                            />
                        </div>
                    </div>

                    <div className="mt-2 sm:flex sm:items-center sm:justify-between">
                        <div
                            className="flex h-[38px] items-center"
                            onClick={(event) => event.preventDefault()}
                        >
                            {connection.tags && (
                                <TagList
                                    id={connection.id!}
                                    remainingTags={remainingTags}
                                    tags={connection.tags}
                                    updateTagsMutation={
                                        updateConnectionTagsMutation
                                    }
                                    getRequest={(id, tags) => ({
                                        id: id!,
                                        updateTagsRequestModel: {
                                            tags: tags || [],
                                        },
                                    })}
                                />
                            )}
                        </div>

                        <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                            {connection.createdDate && (
                                <>
                                    <CalendarIcon
                                        className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                        aria-hidden="true"
                                    />

                                    <span>
                                        {`Created ${connection.createdDate?.toLocaleDateString()} ${connection.createdDate?.toLocaleTimeString()}`}
                                    </span>
                                </>
                            )}
                        </div>
                    </div>
                </div>

                <div className="flex items-center"></div>

                <DropdownMenu id={connection.id} menuItems={menuItems} />
            </div>

            {showDeleteDialog && (
                <AlertDialog
                    danger
                    isOpen
                    message="This action cannot be undone. This will permanently delete the connection."
                    title="Are you absolutely sure?"
                    setIsOpen={setShowDeleteDialog}
                    onConfirmClick={() => {
                        if (connection.id) {
                            deleteConnectionMutation.mutate(connection.id);
                        }
                    }}
                />
            )}

            {showEditDialog && (
                <ConnectionDialog
                    connection={connection}
                    showTrigger={false}
                    visible
                    onClose={() => setShowEditDialog(false)}
                />
            )}
        </>
    );
};

export default ConnectionListItem;
