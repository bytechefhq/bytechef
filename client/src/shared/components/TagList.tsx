import Button from '@/components/Button/Button';
import CreatableSelect, {SelectOptionType} from '@/components/CreatableSelect/CreatableSelect';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronDownIcon, TagIcon, XIcon} from 'lucide-react';
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
    <div className="group relative flex max-h-8 items-center rounded-full border border-stroke-neutral-tertiary px-2 text-xs text-content-neutral-primary">
        <span className="py-0.5">{tag.name}</span>

        <Tooltip>
            <TooltipTrigger asChild>
                <Button
                    aria-label="Remove tag"
                    className="pointer-events-none absolute top-1/2 right-0.5 size-4 -translate-y-1/2 rounded-full bg-surface-neutral-tertiary p-0.5 opacity-0 transition-opacity group-hover:pointer-events-auto group-hover:opacity-100 hover:bg-surface-destructive-secondary-hover focus-visible:pointer-events-auto focus-visible:opacity-100 dark:bg-surface-neutral-tertiary-hover dark:hover:bg-surface-destructive-secondary-hover [&_svg]:size-2.5"
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
        const newTags =
            tags?.filter((tag) => (tag.id != null ? tag.id !== deletedTag.id : tag.name !== deletedTag.name)) || [];

        updateTagsMutation.mutate(getRequest(id, newTags));
    };

    return (
        <div className="mr-4 flex h-7 items-center justify-center gap-1.5">
            {tags.length > 0 && <TagIcon aria-label="Tags" className="size-3.5 text-content-neutral-secondary" />}

            {tags.slice(0, 3).map((tag) => (
                <Tag key={tag.id ?? tag.name} onDeleteTag={handleDeleteTag} tag={tag} />
            ))}

            {tags.length > 3 && (
                <Popover>
                    <PopoverTrigger asChild>
                        <Button
                            className="data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                            icon={<ChevronDownIcon />}
                            onClick={() => setShowAllTags(!showAllTags)}
                            size="iconXs"
                            variant="ghost"
                        />
                    </PopoverTrigger>

                    <PopoverContent align="end" className="w-min p-2">
                        <div className="flex w-min flex-col space-y-1">
                            {tags.slice(3).map((tag) => (
                                <Tag key={tag.id ?? tag.name} onDeleteTag={handleDeleteTag} tag={tag} />
                            ))}
                        </div>
                    </PopoverContent>
                </Popover>
            )}

            {isNewTagWindowVisible ? (
                <>
                    <CreatableSelect
                        className="w-52 text-start"
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
                            placeholder: (styles: any) => ({
                                ...styles,
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                            }),
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
                <Button
                    className="rounded-full border border-dashed border-stroke-neutral-tertiary text-content-neutral-secondary hover:border-solid hover:text-content-neutral-primary"
                    label="Add tag"
                    onClick={(event) => {
                        event.preventDefault();

                        setIsNewTagWindowVisible(true);
                    }}
                    size="xs"
                    variant="ghost"
                />
            )}
        </div>
    );
};

export default TagList;
