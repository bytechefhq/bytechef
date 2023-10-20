import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    button: string;
    category: string;
    date: string;
    id: string;
    name: string;
    status: string;
    tag: string;
}> = ({id, name, status, category, tag, button, date}) => {
    return (
        <div>
            <Header id={id} name={name} status={status} />

            <Footer category={category} tag={tag} button={button} date={date} />
        </div>
    );
};
