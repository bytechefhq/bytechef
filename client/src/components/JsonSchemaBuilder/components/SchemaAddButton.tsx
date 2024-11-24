import {PlusIcon} from 'lucide-react';

import SchemaRoundedButton from './SchemaRoundedButton';

interface AddButtonProps {
    onClick?: () => void;
    title?: string;
}

const SchemaAddButton = ({onClick = () => {}, title}: AddButtonProps) => {
    return (
        <SchemaRoundedButton
            className="text-primary hover:bg-accent hover:text-accent-foreground"
            onClick={onClick}
            title={title}
        >
            <PlusIcon className="h-4" />
        </SchemaRoundedButton>
    );
};

export default SchemaAddButton;
