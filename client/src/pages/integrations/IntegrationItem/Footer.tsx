import React from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';
import {CategoryModel, TagModel} from '../../../data-access/integration';

const Footer: React.FC<{
    category?: CategoryModel;
    tags?: TagModel[];
    date?: Date;
    remainingTags?: TagModel[];
    onAddTag: (newTag: TagModel) => void;
    onDeleteTag: (deletedTag: TagModel) => void;
}> = ({category, tags, date, remainingTags, onAddTag, onDeleteTag}) => {
    return (
        <div className="flex">
            {category && <Category category={category} />}

            {tags && (
                <TagList
                    tags={tags}
                    remainingTags={remainingTags}
                    onAddTag={onAddTag}
                    onDeleteTag={onDeleteTag}
                />
            )}

            {date && <Date date={date} />}
        </div>
    );
};

export default Footer;
