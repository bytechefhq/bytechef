import React, {useState} from 'react';
import {
    Dropdown,
    DropdownMenuItemType,
} from '../../components/Dropdown/Dropdown';
import {TagModel} from '../../middleware/connection';
import {useQueryClient} from '@tanstack/react-query';
import CreatableSelect, {
    SelectOption,
} from '../../components/CreatableSelect/CreatableSelect';
import Button from 'components/Button/Button';
import {PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import {ChevronDownIcon, Component1Icon} from '@radix-ui/react-icons';
import {ConnectionKeys} from '../../queries/connections';
import {
    useConnectionDeleteMutation,
    useConnectionTagsMutation,
} from '../../mutations/connections.mutations';
import {ComponentDefinitionKeys} from '../../queries/componentDefinitions';
import {SingleValue} from 'react-select';

interface ConnectionItemProps {
    id?: number;
    lastModifiedDate?: Date;
    name: string;
    remainingTags?: TagModel[];
    tags?: TagModel[];
    version?: number;
}

interface DateProps {
    lastModifiedDate?: Date;
}

interface FooterProps {
    tags?: TagModel[];
    remainingTags?: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
}

interface HeaderProps {
    name: string;
}

interface NameProps {
    name: string;
}

const Date = ({lastModifiedDate}: DateProps) => {
    return (
        <span className="mr-4 text-center text-sm text-gray-500">
            Last Modified{' '}
            {`${lastModifiedDate?.toLocaleDateString()} ${lastModifiedDate?.toLocaleTimeString()}`}
        </span>
    );
};

const Footer = ({tags, remainingTags, onAddTag, onDeleteTag}: FooterProps) => {
    return (
        <div
            className="flex h-[38px] items-center"
            onClick={(event) => event.preventDefault()}
        >
            {tags && (
                <TagList
                    tags={tags}
                    remainingTags={remainingTags}
                    onAddTag={onAddTag}
                    onDeleteTag={onDeleteTag}
                />
            )}
        </div>
    );
};

const Header = ({name}: HeaderProps) => {
    return (
        <div className="relative mb-2 flex items-center">
            <Component1Icon className="mr-1 h-5 w-5 flex-none" />{' '}
            <Name name={name} />
        </div>
    );
};

const ConnectionItem = ({
    id,
    lastModifiedDate,
    name,
    tags,
    remainingTags,
}: ConnectionItemProps) => {
    const queryClient = useQueryClient();

    const connectionDeleteMutation = useConnectionDeleteMutation({
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

    const menuItems: DropdownMenuItemType[] = [
        {
            label: 'Edit',
        },
        {
            separator: true,
        },
        {
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
                <Header name={name} />

                <Footer
                    tags={tags}
                    remainingTags={remainingTags}
                    onAddTag={handleOnAddTag}
                    onDeleteTag={handleOnDeleteTag}
                />
            </div>

            <div className="flex items-center">
                <Date lastModifiedDate={lastModifiedDate} />

                <Dropdown id={id} menuItems={menuItems} />
            </div>
        </div>
    );
};

const Name = ({name}: NameProps) => {
    return (
        <span className="mr-2 text-base font-semibold text-gray-900">
            {name}
        </span>
    );
};

export default ConnectionItem;
