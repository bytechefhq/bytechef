import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {ComponentIcon} from 'lucide-react';
import {useState} from 'react';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const [_, setIsHovered] = useState(false);

    const {data: componentDefinition} = useGetComponentDefinitionQuery({
        componentName: mcpComponent.componentName,
        componentVersion: mcpComponent.componentVersion,
    });

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
            <div className="flex flex-1 cursor-pointer items-center gap-x-2" onClick={handleComponentClick}>
                {componentDefinition?.icon ? (
                    <img alt={`${mcpComponent.componentName} icon`} className="size-4" src={componentDefinition.icon} />
                ) : (
                    <ComponentIcon className="size-4 text-gray-500" />
                )}

                <span className="min-w-60 text-sm font-semibold">{mcpComponent.componentName}</span>

                {mcpComponent.mcpTools && mcpComponent.mcpTools.length > 0 && (
                    <div className="flex items-center gap-x-1">
                        <div className="flex items-center gap-x-1">
                            {mcpComponent.mcpTools
                                .slice(0, 4)
                                .map(
                                    (tool) =>
                                        tool?.name && (
                                            <Badge
                                                key={tool.name}
                                                label={tool.name}
                                                styleType="secondary-filled"
                                                weight="semibold"
                                            />
                                        )
                                )}

                            {mcpComponent.mcpTools.length > 4 && (
                                <span className="text-xs text-gray-500">+{mcpComponent.mcpTools.length - 4} more</span>
                            )}
                        </div>
                    </div>
                )}
            </div>

            <div className="flex items-center gap-x-4">
                <McpComponentListItemDropdownMenu mcpComponent={mcpComponent} mcpServer={mcpServer} />
            </div>
        </li>
    );
};

export default McpComponentListItem;
