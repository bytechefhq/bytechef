import {Button} from '@/components/ui/button';
import {ChevronDownIcon, Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import CreatableSelect, {SelectOptionType} from 'components/CreatableSelect/CreatableSelect';
import {useState} from 'react';
import {OnChangeValue} from 'react-select';

type TagModelType = {
    readonly createdBy?: string;
    readonly createdDate?: Date;
    id?: number;
    readonly lastModifiedBy?: string;
    readonly lastModifiedDate?: Date;
    name: string;
    version?: number;
};

interface TagProps {
    tag: TagModelType;
    onDeleteTag: (deletedTag: TagModelType) => void;
}

const Tag = ({onDeleteTag, tag}: TagProps) => (
    <span className="inline-flex items-center rounded-full border border-muted px-2 py-1 text-xs text-gray-700 hover:border-gray-200">
        {tag.name}

        <Cross2Icon
            className="ml-1.5 size-3 cursor-pointer rounded-full hover:bg-gray-200"
            onClick={() => onDeleteTag(tag)}
        />
    </span>
);

interface TagListProps {
    id: number;
    remainingTags?: Array<TagModelType>;
    tags: Array<TagModelType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateTagsMutation: UseMutationResult<void, object, any, unknown>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    getRequest: (id: number, tags: Array<TagModelType>) => any;
}

const TagList = ({getRequest, id, remainingTags, tags, updateTagsMutation}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    const handleAddTag = (newTag: TagModelType) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        updateTagsMutation.mutate(getRequest(id, newTags));
    };

    const handleDeleteTag = (deletedTag: TagModelType) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        updateTagsMutation.mutate(getRequest(id, newTags));
    };

    return (
        <div className="mr-4 flex items-center space-x-1">
            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} onDeleteTag={handleDeleteTag} tag={tag} />
            ))}

            {tags.length > 3 && (
                <div className="relative flex">
                    <Button className="mr-2" onClick={() => setShowAllTags(!showAllTags)} size="icon" variant="ghost">
                        <ChevronDownIcon />
                    </Button>

                    {showAllTags && (
                        <div
                            className="absolute left-0 top-full z-10 w-px space-y-2 border-0 bg-white shadow-lg"
                            style={{maxHeight: '32px'}}
                        >
                            {tags.slice(3).map((tag) => (
                                <Tag key={tag.id} onDeleteTag={handleDeleteTag} tag={tag} />
                            ))}
                        </div>
                    )}
                </div>
            )}

            {isNewTagWindowVisible ? (
                <CreatableSelect
                    className="w-40 text-start"
                    name="newTag"
                    onChange={(selectedOption: OnChangeValue<SelectOptionType, false>) => {
                        if (selectedOption) {
                            handleAddTag(selectedOption.tag);
                        }

                        setIsNewTagWindowVisible(false);
                    }}
                    onCreateOption={(inputValue: string) => {
                        handleAddTag({
                            name: inputValue,
                        });

                        setIsNewTagWindowVisible(false);
                    }}
                    options={remainingTags!.map((tag: TagModelType) => ({
                        label: `${tag.name}`,
                        tag,
                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                    }))}
                />
            ) : (
                <div
                    className="flex size-6 cursor-pointer items-center justify-center rounded border border-gray-100 hover:bg-gray-200"
                    onClick={(event) => {
                        event.preventDefault();

                        setIsNewTagWindowVisible(true);
                    }}
                >
                    <PlusIcon className="size-3" />
                </div>
            )}
        </div>
    );
};

export default TagList;
