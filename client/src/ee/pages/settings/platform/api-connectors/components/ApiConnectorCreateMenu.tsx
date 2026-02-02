import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {ChevronDownIcon, FileUpIcon, LinkIcon, PlusIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import {useApiConnectorWizardStore} from '../stores/useApiConnectorWizardStore';

const ApiConnectorCreateMenu = () => {
    const reset = useApiConnectorWizardStore((state) => state.reset);
    const navigate = useNavigate();

    const handleMenuItemClick = (path: string) => {
        reset();
        navigate(path);
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    Create API Connector
                    <ChevronDownIcon className="ml-2 size-4" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem
                    onSelect={() => handleMenuItemClick('/automation/settings/api-connectors/new/manual')}
                >
                    <PlusIcon className="mr-2 size-4" />
                    Create API Connector
                </DropdownMenuItem>

                <DropdownMenuItem
                    onSelect={() => handleMenuItemClick('/automation/settings/api-connectors/new/import')}
                >
                    <FileUpIcon className="mr-2 size-4" />
                    Import Open API file
                </DropdownMenuItem>

                <DropdownMenuItem onSelect={() => handleMenuItemClick('/automation/settings/api-connectors/new/ai')}>
                    <LinkIcon className="mr-2 size-4" />
                    Create from documentation URL
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ApiConnectorCreateMenu;
