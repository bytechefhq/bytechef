import React from 'react';
import {Category} from './Category';
import {Date} from './Date';
import {TagList} from './TagList';

const Footer: React.FC<{
    category: string;
    tag: string;
    button: string;
    date: string;
}> = ({category, tag, button, date}) => {
    return (
        <div>
            <Category category={category} />

            <TagList tag={tag} button={button} />

            <Date date={date} />
        </div>
    );
};

export default Footer;
