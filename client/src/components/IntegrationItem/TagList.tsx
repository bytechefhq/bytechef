import React from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const TagList: React.FC<{tags: TagModel[]}> = ({tags}) => {
    return (
        <div className="mx-4 flex grow space-x-2 overflow-x-scroll pt-2">
            {tags.map((tag) => (
                <Tag key={tag.id} tag={tag} />
            ))}

            <div className="self-center">
                <PlusIcon />
            </div>
        </div>
    );
};
