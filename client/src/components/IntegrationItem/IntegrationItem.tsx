import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    id: string;
    name: string;
    status: string;
    dropdownTrigger: string;
    category: string;
    tag: string;
    button: string;
    date: string;
}> = ({id, name, status, dropdownTrigger, category, tag, button, date}) => {
    return (
        <div>
            <Header
                id={id}
                name={name}
                status={status}
                dropdownTrigger={dropdownTrigger}
            />

            <Footer category={category} tag={tag} button={button} date={date} />
        </div>
    );
};
