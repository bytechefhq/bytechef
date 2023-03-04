import React from 'react';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../../components/DropdownMenu/DropdownMenu';
import {ConnectionModel, TagModel} from '../../../middleware/connection';
import {useQueryClient} from '@tanstack/react-query';
import {Component1Icon} from '@radix-ui/react-icons';
import {ConnectionKeys} from '../../../queries/connections.queries';
import {
    useDeleteConnectionMutation,
    useUpdateConnectionTagsMutation,
} from '../../../mutations/connections.mutations';
import {ComponentDefinitionKeys} from '../../../queries/componentDefinitions.queries';
import TagList from '../../../components/TagList/TagList';

interface ConnectionItemProps {
    connection: ConnectionModel;
    remainingTags?: TagModel[];
}

const ConnectionItem = ({connection, remainingTags}: ConnectionItemProps) => {
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
        },
        {
            separator: true,
        },
        {
            danger: true,
            label: 'Delete',
            onClick: (id?: number) => {
                if (id) {
                    deleteConnectionMutation.mutate(id);
                }
            },
        },
    ];

    return (
        <div className="flex items-center justify-between">
            <div>
                <div className="relative mb-2 flex items-center">
                    <Component1Icon className="mr-1 h-5 w-5 flex-none" />

                    <span className="mr-2 text-base font-semibold text-gray-900">
                        {connection.name}
                    </span>
                </div>

                <div
                    className="flex h-[38px] items-center"
                    onClick={(event) => event.preventDefault()}
                >
                    {connection.tags && (
                        <TagList
                            id={connection.id!}
                            remainingTags={remainingTags}
                            tags={connection.tags}
                            updateTagsMutation={updateConnectionTagsMutation}
                            getRequest={(id, tags) => ({
                                id: id!,
                                updateConnectionTagsRequestModel: {
                                    tags: tags || [],
                                },
                            })}
                        />
                    )}
                </div>
            </div>

            <div className="flex items-center">
                {connection.lastModifiedDate && (
                    <span className="mr-4 text-center text-sm text-gray-500">
                        {`Last Modified ${connection.lastModifiedDate?.toLocaleDateString()} ${connection.lastModifiedDate?.toLocaleTimeString()}`}
                    </span>
                )}

                <DropdownMenu id={connection.id} menuItems={menuItems} />
            </div>
        </div>
    );
};

export default ConnectionItem;
