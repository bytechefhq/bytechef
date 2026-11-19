import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import useMcpComponentListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpComponentListItem';
import {McpComponent, McpServer} from '@/shared/middleware/graphql';

import McpComponentDialog from '../mcp-component-dialog/McpComponentDialog';
import McpComponentCollapsible from './McpComponentCollapsible';
import McpComponentToolList from './McpComponentToolList';

const McpComponentListItem = ({mcpComponent, mcpServer}: {mcpComponent: McpComponent; mcpServer: McpServer}) => {
    const {componentDefinition, setShowEditDialog, showEditDialog} = useMcpComponentListItem(
        mcpComponent.componentName,
        mcpComponent.componentVersion
    );

    return (
        <McpComponentCollapsible
            dialog={
                <McpComponentDialog
                    mcpComponent={mcpComponent}
                    mcpServerId={mcpServer.id}
                    onOpenChange={setShowEditDialog}
                    open={showEditDialog}
                />
            }
            dropdownMenu={
                <McpComponentListItemDropdownMenu
                    mcpComponent={mcpComponent}
                    onEditClick={() => setShowEditDialog(true)}
                />
            }
            iconSrc={componentDefinition?.icon}
            mcpComponent={mcpComponent}
        >
            <McpComponentToolList
                componentName={mcpComponent.componentName}
                componentVersion={mcpComponent.componentVersion}
                connectionId={mcpComponent.connectionId}
                mcpTools={mcpComponent.mcpTools}
                renderEditDialog={(open, onOpenChange) => (
                    <McpComponentDialog
                        mcpComponent={mcpComponent}
                        mcpServerId={mcpServer.id!}
                        onOpenChange={onOpenChange}
                        open={open}
                    />
                )}
            />
        </McpComponentCollapsible>
    );
};

export default McpComponentListItem;
