import React from 'react';
import {AddTagButton} from './AddTagButton';
import {Tag} from './Tag';

export const TagList: React.FC<{tags: string[]}> = ({tags}) => {
    return (
        <div>
            {tags.map((tag) => (
                <Tag tag={tag} />
            ))}

            <AddTagButton label={'+'} />
        </div>
    );
};
