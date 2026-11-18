import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {API_CONNECTORS_SETTINGS_PATH} from '@/shared/navigation/settingsPaths';
import {ChevronDownIcon, FileUpIcon, LinkIcon, PlusIcon} from 'lucide-react';

import useApiConnectorCreateMenu from './hooks/useApiConnectorCreateMenu';

const ApiConnectorCreateMenu = () => {
    const {handleMenuItemClick} = useApiConnectorCreateMenu();

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    Create API Connector
                    <ChevronDownIcon className="ml-2 size-4" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTORS_SETTINGS_PATH}/new/manual`)}>
                    <PlusIcon className="mr-2 size-4" />
                    Create API Connector
                </DropdownMenuItem>

                <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTORS_SETTINGS_PATH}/new/import`)}>
                    <FileUpIcon className="mr-2 size-4" />
                    Import Open API file
                </DropdownMenuItem>

                <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTORS_SETTINGS_PATH}/new/ai`)}>
                    <LinkIcon className="mr-2 size-4" />
                    Create from documentation URL
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ApiConnectorCreateMenu;
