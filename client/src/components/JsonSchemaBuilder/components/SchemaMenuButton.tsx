import {EllipsisVerticalIcon} from 'lucide-react';
import React from 'react';

import SchemaRoundedButton from './SchemaRoundedButton';

interface MenuButtonProps {
    onClick?: () => void;
    title?: string;
}

const SchemaMenuButton = ({onClick = () => {}, title}: MenuButtonProps) => {
    return (
        <SchemaRoundedButton className="bg-white text-gray-800 hover:bg-gray-200" onClick={onClick} title={title}>
            <EllipsisVerticalIcon className="h-4" />
        </SchemaRoundedButton>
    );
};

export default SchemaMenuButton;
