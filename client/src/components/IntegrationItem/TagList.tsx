import React from 'react';
import {AddTagButton} from './AddTagButton';
import {Tag} from './Tag';

export const TagList: React.FC<{tags: string[]}> = ({tags}) => {
    const handleDeleteTag = (tagName: string) => {
        let handleDelete = [...tags];

        handleDelete.find;
    };

    return (
        <span>
            {tags.map((tag) => (
                <Tag tag={tag} handleDeleteTag={handleDeleteTag} />
            ))}

            <AddTagButton label={'+'} />
        </span>
    );
};
