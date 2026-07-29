import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useApiConnectorCreateMenu from '@/ee/pages/settings/platform/api-connectors/components/hooks/useApiConnectorCreateMenu';
import CreateCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/CreateCustomComponentDialog';
import UploadCustomComponentDialog from '@/ee/pages/settings/platform/custom-components/components/UploadCustomComponentDialog';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {ChevronDownIcon, FileUpIcon, LinkIcon, PlusIcon, UploadIcon} from 'lucide-react';

const API_CONNECTOR_NEW_BASE_PATH = '/automation/settings/components/api-connectors/new';

/**
 * The single create entry point for the merged Components page: custom-component creation/upload (dialogs) and the
 * three API-connector wizard routes, gated by their respective feature flags. The dialog triggers use
 * `onSelect={preventDefault}` so the dropdown stays mounted while the dialog portal is open — closing the menu would
 * unmount the dialog with it.
 */
const NewComponentMenu = () => {
    const isFeatureFlagEnabled = useFeatureFlagsStore();

    const {handleMenuItemClick} = useApiConnectorCreateMenu();

    const apiConnectorsEnabled = isFeatureFlagEnabled('ff-207');
    const customComponentsEnabled = isFeatureFlagEnabled('ff-1024');

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button>
                    New Component
                    <ChevronDownIcon className="ml-2 size-4" />
                </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end">
                {customComponentsEnabled && (
                    <>
                        <CreateCustomComponentDialog
                            trigger={
                                <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                                    <PlusIcon className="mr-2 size-4" />
                                    Custom Component
                                </DropdownMenuItem>
                            }
                        />

                        <UploadCustomComponentDialog
                            trigger={
                                <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                                    <UploadIcon className="mr-2 size-4" />
                                    Import Custom Component
                                </DropdownMenuItem>
                            }
                        />
                    </>
                )}

                {apiConnectorsEnabled && customComponentsEnabled && <DropdownMenuSeparator />}

                {apiConnectorsEnabled && (
                    <>
                        <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTOR_NEW_BASE_PATH}/manual`)}>
                            <PlusIcon className="mr-2 size-4" />
                            API Connector
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTOR_NEW_BASE_PATH}/import`)}>
                            <FileUpIcon className="mr-2 size-4" />
                            Import Open API File
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => handleMenuItemClick(`${API_CONNECTOR_NEW_BASE_PATH}/ai`)}>
                            <LinkIcon className="mr-2 size-4" />
                            API Connector from Docs URL
                        </DropdownMenuItem>
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default NewComponentMenu;
