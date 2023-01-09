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
        <>
            <div className="... flex flex-row">
                <div className="w-24">
                    <Category category={category} />
                </div>

                <div className="w-96">
                    <TagList tags={tags} />
                </div>

                <div className="w-96">
                    <Date date={date} />
                </div>
            </div>
        </>
    );
};

export default Footer;
