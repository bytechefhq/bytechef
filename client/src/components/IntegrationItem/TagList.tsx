import React from 'react';

import {Tag} from './Tag';
import {PlusIcon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const TagList: React.FC<{tags?: TagModel[]}> = ({tags}) => {
    return (
        <div className="flex flex-row space-x-2">
            {tags && tags.map((tag) => <Tag key={tag?.id} tag={tag} />)}

            <div className="self-center">
                <PlusIcon />
            </div>
        </div>
    );
};
