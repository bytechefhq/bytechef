import React from 'react';
import Footer from './Footer';
import Header from './Header';
import {CategoryModel, TagModel} from '../../data-access/integration';

export const IntegrationItem: React.FC<{
    button: string;
    name: string;
    status: boolean;
    description?: string;
    category?: CategoryModel;
    date?: Date;
    id?: number;
    tags?: TagModel[];
    workflowIds?: string[];
}> = ({id, name, status, description, category, tags, date}) => {
    return (
        <div>
            <Header
                id={id}
                name={name}
                status={status}
                description={description}
            />

            <Footer category={category} tags={tags} date={date} />
        </div>
    );
};
