import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    id: string;
    name: string;
    status: boolean;
    description: string;
    category: string;
    tags: string[];
    date: string;
    workflowIds: string[];
}> = ({id, name, status, category, tags, date}) => {
    return (
        <div>
            <Header name={name} status={status} dropdownTrigger={id} />
            <Footer category={category} tags={tags} date={date} />
        </div>
    );
};
