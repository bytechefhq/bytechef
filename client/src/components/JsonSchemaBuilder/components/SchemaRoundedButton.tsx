import React from 'react';

interface RoundedButtonProps {
    onClick?: () => void;
    className?: string;
    title?: string;
    children: React.ReactNode;
}

const SchemaRoundedButton = ({children, className = '', onClick = () => {}, title}: RoundedButtonProps) => {
    return (
        <button
            className={`flex size-8 items-center justify-center rounded-full border text-sm focus:outline-none ${className}`}
            onClick={onClick}
            title={title}
        >
            {children}
        </button>
    );
};

export default SchemaRoundedButton;
