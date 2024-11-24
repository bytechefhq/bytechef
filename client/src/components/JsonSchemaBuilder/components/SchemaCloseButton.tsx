import {XIcon} from 'lucide-react';
import React from 'react';

import SchemaRoundedButton from './SchemaRoundedButton';

interface CloseButtonProps {
    onClick?: () => void;
    title?: string;
}

const SchemaCloseButton = ({onClick = () => {}, title}: CloseButtonProps) => {
    return (
        <SchemaRoundedButton className="bg-white" onClick={onClick} title={title}>
            <XIcon className="h-4" />
        </SchemaRoundedButton>
    );
};

export default SchemaCloseButton;
