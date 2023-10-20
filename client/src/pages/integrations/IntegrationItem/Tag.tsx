import React from 'react';
import {Cross1Icon} from '@radix-ui/react-icons';
import {TagModel} from '../../../data-access/integration';

export const Tag: React.FC<{
    tag: TagModel;
    onDeleteTag: (deletedTag: TagModel) => void;
}> = ({tag, onDeleteTag}) => {
    return (
        <span className="inline-flex items-center rounded-full bg-gray-200 px-3 py-1 text-xs text-gray-700">
            {tag.name}

            <Cross1Icon
                className="ml-1.5 h-3 w-3 rounded-full hover:bg-gray-400"
                onClick={() => onDeleteTag(tag)}
            />
        </span>
    );
};
