import '@/shared/styles/dropdownMenu.css';
import {Badge} from '@/components/ui/badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import {McpComponentType} from '@/shared/queries/platform/mcpComponents.queries';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import {ComponentIcon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

const McpComponentListItem = ({
    mcpComponent,
    mcpServer,
}: {
    mcpComponent: McpComponentType;
    mcpServer: McpServerType;
}) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [_, setIsHovered] = useState(false);

    const handleComponentClick = () => {
        // Add functionality here if needed, similar to handleWorkflowClick in ProjectDeploymentWorkflowListItem
        console.log('Component clicked:', mcpComponent);
    };

    return (
        <li
            className="flex items-center justify-between rounded-md px-2 py-1 hover:bg-gray-50"
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <div className="flex flex-1 cursor-pointer items-center" onClick={handleComponentClick}>
                <span className={twMerge('w-80 text-sm font-semibold')}>{mcpComponent.componentName}</span>

                <div className="ml-6 flex space-x-1">
                    <Tooltip>
                        <TooltipTrigger>
                            <ComponentIcon className="mr-2 size-4 text-gray-500" />
                        </TooltipTrigger>

                        <TooltipContent>Component</TooltipContent>
                    </Tooltip>

                    <Badge className="ml-2" variant="outline">
                        v{mcpComponent.componentVersion}
                    </Badge>

                    {mcpComponent.connectionId && (
                        <Badge className="ml-2" variant="secondary">
                            Connection
                        </Badge>
                    )}
                </div>
            </div>

            <div className="flex items-center gap-x-4">
                <McpComponentListItemDropdownMenu mcpComponent={mcpComponent} mcpServer={mcpServer} />
            </div>
        </li>
    );
};

export default McpComponentListItem;
