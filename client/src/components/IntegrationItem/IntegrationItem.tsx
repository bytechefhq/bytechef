import React from 'react';
import Footer from './Footer';
import Header from './Header';

export const IntegrationItem: React.FC<{
    name: string;
    status: string;
    dropdownTrigger: string;
    category: string;
    tag: string;
    button: string;
    date: string;
}> = ({name, status, dropdownTrigger, category, tag, button, date}) => {
    return (
        <div>
            <Header
                name={name}
                status={status}
                dropdownTrigger={dropdownTrigger}
            />
            <Footer category={category} tag={tag} button={button} date={date} />
        </div>
    );
};
