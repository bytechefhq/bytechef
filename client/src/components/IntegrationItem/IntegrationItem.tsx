import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    button: string;
    category: string;
    date: string;
    id: string;
    name: string;
    status: boolean;
    description: string;
    tags: string[];
    workflowIds: string[];
}> = ({id, name, status, category, tags, date}) => {
    return (
        <div>
            <Header id={id} name={name} status={status} />
            <Footer category={category} tags={tags} date={date} />
        </div>
    );
};
