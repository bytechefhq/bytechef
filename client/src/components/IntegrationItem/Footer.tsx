import React from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';

const Footer: React.FC<{
    category?: string;
    tags?: string[];
    date?: Date;
}> = ({category, tags, date}) => {
    return (
        <div className="grid grid-cols-3 gap-4">
            <div className="w-96">
                <Category category={category} />
            </div>

            <div className="... flex justify-start">
                <TagList tags={tags} />
            </div>

            <div className="... grid justify-items-end">
                <Date date={date} />
            </div>
        </div>
    );
};

export default Footer;
