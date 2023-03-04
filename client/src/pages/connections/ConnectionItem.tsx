import React from 'react';
import DropdownMenu, {
    IDropdownMenuItem,
} from '../../components/DropdownMenu/DropdownMenu';
import {ConnectionModel, TagModel} from '../../middleware/connection';
import {useQueryClient} from '@tanstack/react-query';
import {Component1Icon} from '@radix-ui/react-icons';
import {ConnectionKeys} from '../../queries/connections';
import {useDeleteConnectionMutation} from '../../mutations/connections.mutations';
import {ComponentDefinitionKeys} from '../../queries/componentDefinitions';
import TagList from './components/TagList';

interface ConnectionItemProps {
    connection: ConnectionModel;
    remainingTags?: TagModel[];
}

const ConnectionItem = ({connection, remainingTags}: ConnectionItemProps) => {
    const queryClient = useQueryClient();

    const connectionDeleteMutation = useDeleteConnectionMutation({
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
                    connectionDeleteMutation.mutate(id);
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
                            connectionId={connection.id!}
                            remainingTags={remainingTags}
                            tags={connection.tags}
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
