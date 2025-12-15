import Button from '@/components/Button/Button';
import CreatableSelect, {SelectOptionType} from '@/components/CreatableSelect/CreatableSelect';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronDownIcon, PlusIcon, XIcon} from 'lucide-react';
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
    <div className="group flex max-h-8 items-center justify-between rounded-full border border-border/50 pl-2 pr-1 text-xs text-gray-700">
        <span className="py-1">{tag.name}</span>

        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    className="ml-1.5 rounded-full opacity-100"
                    icon={<XIcon />}
                    onClick={() => onDeleteTag(tag)}
                    size="iconXxs"
                    variant="destructiveGhost"
                />
            </TooltipTrigger>

            <TooltipContent>Remove tag</TooltipContent>
        </Tooltip>
    </div>
);

interface TagListProps {
    id: number;
    remainingTags?: Array<TagType>;
    tags: Array<TagType>;
    /* eslint-disable @typescript-eslint/no-explicit-any */
    updateTagsMutation: any;
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
        <div className="mr-4 flex h-7 items-center justify-center gap-2">
            <span className="text-xs text-gray-500">Tags:</span>

            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id} onDeleteTag={handleDeleteTag} tag={tag} />
            ))}

            {tags.length > 3 && (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="[&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary"
                            icon={<ChevronDownIcon />}
                            onClick={() => setShowAllTags(!showAllTags)}
                            size="iconXs"
                            variant="ghost"
                        />
                    </PopoverTrigger>

                    <PopoverContent align="end" className="w-min p-2">
                        <div className="flex w-min flex-col space-y-1">
                            {tags.slice(3).map((tag) => (
                                <Tag key={tag.id} onDeleteTag={handleDeleteTag} tag={tag} />
                            ))}
                        </div>
                    </PopoverContent>
                </Popover>
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
                            control: (styles: any) => ({
                                ...styles,
                                fontSize: '0.75rem',
                                minHeight: '1.5rem',
                                padding: '0',
                            }),
                            dropdownIndicator: (styles: any) => ({...styles, cursor: 'pointer', padding: '0.2rem'}),
                            valueContainer: (styles: any) => ({...styles, padding: '0 0.4rem'}),
                        }}
                    />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className="opacity-100"
                                icon={<XIcon />}
                                onClick={() => setIsNewTagWindowVisible(false)}
                                size="iconSm"
                                variant="destructiveGhost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Cancel adding a new tag</TooltipContent>
                    </Tooltip>
                </>
            ) : (
                <Tooltip>
                    <TooltipTrigger asChild>
                        <Button
                            icon={<PlusIcon />}
                            onClick={(event) => {
                                event.preventDefault();

                                setIsNewTagWindowVisible(true);
                            }}
                            size="iconXs"
                            variant="ghost"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Add new tag</TooltipContent>
                </Tooltip>
            )}
        </div>
    );
};

export default TagList;
