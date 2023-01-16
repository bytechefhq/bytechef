import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    button: string;
    name: string;
    status: boolean;
    category?: string;
    date?: Date;
    id?: number;
    description?: string;
    tags?: string[];
    workflowIds?: string[];
}> = ({id, name, status, category, tags, date}) => {
    return (
        <div>
            <Header id={id} name={name} status={status} />

            <Footer category={category} tags={tags} date={date} />
        </div>
    );
};
