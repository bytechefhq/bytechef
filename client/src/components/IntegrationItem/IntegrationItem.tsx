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
        <ul role="list" className="space-y-3">
            <li className="overflow-hidden rounded-md bg-white px-6 py-4 shadow">
                <Header id={id} name={name} status={status} />

                <Footer category={category} tags={tags} date={date} />
            </li>
        </ul>
    );
};
