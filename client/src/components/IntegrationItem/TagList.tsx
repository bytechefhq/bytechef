import React from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';

export const TagList: React.FC<{tags?: string[]}> = ({tags}) => {
    return (
        <div className="flex flex-row space-x-2">
            {tags && tags.map((tag) => <Tag key={tag} tag={tag} />)}

            <div className="self-center">
                <PlusIcon />
            </div>
        </div>
    );
};
