import React, {useState} from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';
import {TagModel} from '../../../data-access/integration';
import CreatableSelect from 'react-select/creatable';

export const TagList: React.FC<{
    tags: TagModel[];
    remainingTags?: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
}> = ({tags, remainingTags, onAddTag, onDeleteTag}) => {
    const [isNewTagWindowVisible, setIsNewTagWindowVisible] = useState(false);

    return (
        <div className="mx-4 flex grow space-x-2 pt-2">
            {tags.map((tag) => (
                <Tag key={tag.id} tag={tag} onDeleteTag={onDeleteTag} />
            ))}

            {!isNewTagWindowVisible ? (
                <div
                    className="self-center"
                    onClick={() => setIsNewTagWindowVisible(true)}
                >
                    <PlusIcon className="rounded-full hover:bg-gray-400" />
                </div>
            ) : (
                <CreatableSelect
                    // className="m-4"
                    name={'newTag'}
                    isMulti={false}
                    options={remainingTags!.map((tag: TagModel) => ({
                        label: `${tag.name
                            .charAt(0)
                            .toUpperCase()}${tag.name.slice(1)}`,
                        value: tag.name.toLowerCase().replace(/\W/g, ''),
                        ...tag,
                    }))}
                    onCreateOption={(inputValue: string) => {
                        onAddTag({
                            name: inputValue,
                        });
                        setIsNewTagWindowVisible(false);
                    }}
                    onChange={(selectedOption) => {
                        remainingTags &&
                            onAddTag(
                                remainingTags.filter(
                                    (tag) =>
                                        tag.id != null &&
                                        tag.id === selectedOption!.id
                                )[0]
                            );
                        setIsNewTagWindowVisible(false);
                    }}
                />
            )}
        </div>
    );
};
