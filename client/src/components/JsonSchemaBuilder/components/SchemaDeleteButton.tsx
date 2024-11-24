import {TrashIcon} from 'lucide-react';

import SchemaRoundedButton from './SchemaRoundedButton';

interface DeleteButtonProps {
    onClick?: () => void;
    title?: string;
}

const SchemaDeleteButton = ({onClick = () => {}, title}: DeleteButtonProps) => {
    return (
        <SchemaRoundedButton className="text-destructive hover:bg-accent" onClick={onClick} title={title}>
            <TrashIcon className="h-4" />
        </SchemaRoundedButton>
    );
};

export default SchemaDeleteButton;
