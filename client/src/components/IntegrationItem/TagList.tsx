import React from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';

export const TagList: React.FC<{tags: string[]}> = ({tags}) => {
    const handleDeleteTag = (tagName: string) => {
        let handleDelete = [...tags];

        handleDelete.find;
    };

    return (
        <span className="... flex flex-row space-x-2">
            {tags.map((tag) => (
                <Tag tag={tag} handleDeleteTag={handleDeleteTag} />
            ))}
            <div className="self-center">
                <PlusIcon />
            </div>
        </span>
    );
};
