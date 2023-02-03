import React from 'react';
import {Cross1Icon} from '@radix-ui/react-icons';
import {TagModel} from '../../data-access/integration';

export const Tag: React.FC<{tag: TagModel}> = ({tag}) => {
    return (
        <span className="inline-flex items-center rounded-full bg-gray-100 px-3 py-1 text-xs text-gray-700 hover:bg-gray-200">
            {tag.name}

            <Cross1Icon className="ml-1.5 h-3 w-3" />
        </span>
    );
};
