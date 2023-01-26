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
        <div className="grid grid-cols-6 gap-4">
            {category && (
                <div className="col-start-1 col-end-2">
                    <Category category={category} />
                </div>
            )}

            {tags && (
                <div className="col-start-2 col-end-6 pt-2">
                    <TagList tags={tags} />
                </div>
            )}

            {date && (
                <div className="col-start-6 col-end-7 grid h-10 justify-items-end">
                    <Date date={date} />
                </div>
            )}
        </div>
    );
};

export default Footer;
