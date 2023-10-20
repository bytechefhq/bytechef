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
        <div className="flex">
            {category && <Category category={category} />}

            {tags && <TagList tags={tags} />}

            {date && <Date date={date} />}
        </div>
    );
};

export default Footer;
