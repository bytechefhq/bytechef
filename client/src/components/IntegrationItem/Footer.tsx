import React from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';

const Footer: React.FC<{
    category: string;
    tags: string[];
    date: string;
}> = ({category, tags, date}) => {
    return (
        <span className="w-96 rounded bg-white shadow">
            <Category category={category} />
            
                <TagList tags={tags} />
        
            <Date date={date} />
        </span>
    );
};

export default Footer;
