import React, {useState} from 'react';
import {Dropdown, DropDownMenuItem} from '../../components/DropDown/Dropdown';
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

interface TagProps {
    tag: TagModel;
    onDeleteTag: (deletedTag: TagModel) => void;
}

interface TagListProps {
    tags: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
    remainingTags?: TagModel[];
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

    const menuItems: DropDownMenuItem[] = [
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

    const tagsMutation = useConnectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);
        },
    });

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        tagsMutation.mutate({
            id: id || 0,
            putConnectionTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        tagsMutation.mutate({
            id: id || 0,
            putConnectionTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

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

const Tag = ({tag, onDeleteTag}: TagProps) => {
    return (
        <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-700">
            {tag.name}

            <XMarkIcon
                className="ml-1.5 h-3 w-3 rounded-full hover:bg-gray-300"
                onClick={() => onDeleteTag(tag)}
            />
        </span>
    );
};

const TagList = ({
    tags,
    remainingTags,
    onAddTag,
    onDeleteTag,
}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    return (
        <div className="mr-4 flex items-center space-x-1">
            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} tag={tag} onDeleteTag={onDeleteTag} />
            ))}

            {tags.length > 3 && (
                <div className="relative flex">
                    <Button
                        className="mr-2"
                        size="small"
                        displayType="unstyled"
                        icon={<ChevronDownIcon />}
                        onClick={() => setShowAllTags(!showAllTags)}
                    />

                    {showAllTags && (
                        <div
                            className="absolute left-0 top-full z-10 w-px space-y-2 border-0 bg-white shadow-lg"
                            style={{maxHeight: '32px'}}
                        >
                            {tags.slice(3).map((tag) => (
                                <Tag
                                    key={tag.id}
                                    tag={tag}
                                    onDeleteTag={onDeleteTag}
                                />
                            ))}
                        </div>
                    )}
                </div>
            )}

            {isNewTagWindowVisible ? (
                <CreatableSelect
                    className="w-40"
                    name="newTag"
                    isMulti={false}
                    options={remainingTags!.map((tag: TagModel) => ({
                        label: `${tag.name
                            .charAt(0)
                            .toUpperCase()}${tag.name.slice(1)}`,
                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                        tag,
                    }))}
                    onCreateOption={(inputValue: string) => {
                        onAddTag({
                            name: inputValue,
                        });
                        setIsNewTagWindowVisible(false);
                    }}
                    onChange={(selectedOption: SingleValue<SelectOption>) => {
                        if (selectedOption) {
                            onAddTag(selectedOption.tag);
                        }

                        setIsNewTagWindowVisible(false);
                    }}
                />
            ) : (
                <div
                    className="flex h-6 w-6 cursor-pointer items-center justify-center rounded bg-gray-100 hover:bg-gray-200"
                    onClick={(event) => {
                        event.preventDefault();
                        setIsNewTagWindowVisible(true);
                    }}
                >
                    <PlusIcon className="h-3 w-3 rounded-full hover:bg-gray-300" />
                </div>
            )}
        </div>
    );
};

export default ConnectionItem;
