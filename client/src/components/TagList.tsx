import {Button} from '@/components/ui/button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronDownIcon, Cross2Icon, PlusIcon} from '@radix-ui/react-icons';
import {UseMutationResult} from '@tanstack/react-query';
import CreatableSelect, {SelectOptionType} from 'components/CreatableSelect/CreatableSelect';
import {useState} from 'react';
import {OnChangeValue} from 'react-select';

type TagType = {
    readonly createdBy?: string;
    readonly createdDate?: Date;
    id?: number;
    readonly lastModifiedBy?: string;
    readonly lastModifiedDate?: Date;
    name: string;
    version?: number;
};

interface TagProps {
    tag: TagType;
    onDeleteTag: (deletedTag: TagType) => void;
}

const Tag = ({onDeleteTag, tag}: TagProps) => (
    <div className="group flex max-h-8 items-center rounded-full border border-muted pl-2 pr-1 text-xs text-gray-700">
        <span className="py-1">{tag.name}</span>

        <Tooltip>
            <TooltipTrigger asChild>
                <button
                    className="ml-1.5 rounded-full p-1 text-red-500 hover:bg-red-100 hover:text-red-700"
                    onClick={() => onDeleteTag(tag)}
                >
                    <Cross2Icon className="size-3 cursor-pointer" />
                </button>
            </TooltipTrigger>

            <TooltipContent>Remove</TooltipContent>
        </Tooltip>
    </div>
);

interface TagListProps {
    id: number;
    remainingTags?: Array<TagType>;
    tags: Array<TagType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateTagsMutation: UseMutationResult<void, object, any, unknown>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    getRequest: (id: number, tags: Array<TagType>) => any;
}

const TagList = ({getRequest, id, remainingTags, tags, updateTagsMutation}: TagListProps) => {
    const [showAllTags, setShowAllTags] = useState(false);
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    const handleAddTag = (newTag: TagType) => {
        const newTags = (tags && [...tags]) || [];

        newTags.push(newTag);

        updateTagsMutation.mutate(getRequest(id, newTags));
    };

    const handleDeleteTag = (deletedTag: TagType) => {
        const newTags = tags?.filter((tag) => tag.id !== deletedTag.id) || [];

        updateTagsMutation.mutate(getRequest(id, newTags));
    };

    return (
        <div className="mr-4 flex items-center space-x-2">
            <span className="text-xs text-gray-500">Tags:</span>

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
                <>
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
                        options={remainingTags!.map((tag: TagType) => ({
                            label: `${tag.name}`,
                            tag,
                            value: tag.name.toLowerCase().replace(/\W/g, ''),
                        }))}
                        styles={{
                            control: (styles) => ({...styles, fontSize: '0.75rem', minHeight: '1.5rem', padding: '0'}),
                            dropdownIndicator: (styles) => ({...styles, padding: '0.2rem'}),
                            valueContainer: (styles) => ({...styles, padding: '0 0.4rem'}),
                        }}
                    />

                    <Tooltip>
                        <TooltipTrigger>
                            <Button
                                className="px-2 text-red-500 hover:bg-red-100 hover:text-red-700"
                                onClick={() => setIsNewTagWindowVisible(false)}
                                size="sm"
                                variant="ghost"
                            >
                                <Cross2Icon />
                            </Button>
                        </TooltipTrigger>

                        <TooltipContent>Cancel adding a new tag</TooltipContent>
                    </Tooltip>
                </>
            ) : (
                <div
                    className="flex size-6 cursor-pointer items-center justify-center rounded border border-gray-100 hover:bg-gray-200"
                    onClick={(event) => {
                        event.preventDefault();

                        setIsNewTagWindowVisible(true);
                    }}
                >
                    <Tooltip>
                        <TooltipTrigger>
                            <PlusIcon className="size-3" />
                        </TooltipTrigger>

                        <TooltipContent>Add new tag</TooltipContent>
                    </Tooltip>
                </div>
            )}
        </div>
    );
};

export default TagList;
