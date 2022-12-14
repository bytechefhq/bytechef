import React from 'react';
import {AddTagButton} from './AddTagButton';
import {Tag} from './Tag';

export const TagList: React.FC<{tag: string; button: string}> = ({
    tag,
    button,
}) => {
    return (
        <div>
            <Tag tag={tag} />
            <AddTagButton button={button} />
        </div>
    );
};
