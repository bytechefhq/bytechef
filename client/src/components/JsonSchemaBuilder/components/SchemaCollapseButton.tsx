import {ChevronRightIcon} from '@radix-ui/react-icons';
import {ChevronDownIcon} from 'lucide-react';

import SchemaRoundedButton from './SchemaRoundedButton';

interface CollapseButtonProps {
    onClick?: () => void;
    isCollapsed?: boolean;
    title?: string;
}

const SchemaCollapseButton = ({isCollapsed = false, onClick = () => {}, title}: CollapseButtonProps) => {
    return (
        <SchemaRoundedButton className="hover:bg-accent hover:text-accent-foreground" onClick={onClick} title={title}>
            {isCollapsed ? <ChevronRightIcon className="h-4" /> : <ChevronDownIcon className="h-4" />}
        </SchemaRoundedButton>
    );
};

export default SchemaCollapseButton;
