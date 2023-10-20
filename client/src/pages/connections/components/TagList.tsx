import React, {useState} from 'react';
import Button from '../../../components/Button/Button';
import {ChevronDownIcon} from '@radix-ui/react-icons';
import CreatableSelect, {
    SelectOption,
} from '../../../components/CreatableSelect/CreatableSelect';
import {TagModel} from '../../../middleware/connection';
import {SingleValue} from 'react-select';
import {PlusIcon, XMarkIcon} from '@heroicons/react/24/outline';
import {useUpdateConnectionTagsMutation} from '../../../mutations/connections.mutations';
import {ConnectionKeys} from '../../../queries/connections';
import {useQueryClient} from '@tanstack/react-query';

interface TagProps {
    tag: TagModel;
    onDeleteTag: (deletedTag: TagModel) => void;
}

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

interface TagListProps {
    connectionId: number;
    remainingTags?: TagModel[];
    tags: TagModel[];
}

const TagList = ({connectionId, tags, remainingTags}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    const queryClient = useQueryClient();

    const updateConnectionTagsMutation = useUpdateConnectionTagsMutation({
        onSuccess: () => {
            queryClient.invalidateQueries(ConnectionKeys.connections);
            queryClient.invalidateQueries(ConnectionKeys.connectionTags);
        },
    });

    const handleOnAddTag = (newTag: TagModel) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        updateConnectionTagsMutation.mutate({
            id: connectionId,
            updateConnectionTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    const handleOnDeleteTag = (deletedTag: TagModel) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        updateConnectionTagsMutation.mutate({
            id: connectionId,
            updateConnectionTagsRequestModel: {
                tags: newTags || [],
            },
        });
    };

    return (
        <div className="mr-4 flex items-center space-x-1">
            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} tag={tag} onDeleteTag={handleOnDeleteTag} />
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
                                    onDeleteTag={handleOnDeleteTag}
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
                        handleOnAddTag({
                            name: inputValue,
                        });
                        setIsNewTagWindowVisible(false);
                    }}
                    onChange={(selectedOption: SingleValue<SelectOption>) => {
                        if (selectedOption) {
                            handleOnAddTag(selectedOption.tag);
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

export default TagList;
