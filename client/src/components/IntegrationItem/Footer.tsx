import React from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';
import {CategoryModel, TagModel} from '../../data-access/integration';

const Footer: React.FC<{
    category?: CategoryModel;
    tags?: TagModel[];
    date?: Date;
}> = ({category, tags, date}) => {
    return (
        <div className="grid grid-cols-3 gap-4">
            {category && (
                <div className="w-96">
                    <Category category={category} />
                </div>
            )}

            {tags && (
                <div className="flex justify-start">
                    <TagList tags={tags} />
                </div>
            )}

            {date && (
                <div className="grid justify-items-end">
                    <Date date={date} />
                </div>
            )}
        </div>
    );
};

export default Footer;
