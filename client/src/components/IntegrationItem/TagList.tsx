import React from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const TagList: React.FC<{tags: TagModel[]}> = ({tags}) => {
    return (
        <div className="mr-4 flex grow space-x-2 overflow-x-scroll pt-2">
            {tags.map((tag) => (
                <Tag key={tag.id} tag={tag} />
            ))}

            <div className="flex h-6 w-6 items-center justify-center self-center rounded hover:bg-gray-100">
                <PlusIcon />
            </div>
        </div>
    );
};
