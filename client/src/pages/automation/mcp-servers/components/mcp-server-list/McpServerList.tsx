import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {McpServerType} from '@/shared/queries/platform/mcpServers.queries';
import McpServerListItem from 'pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';

import McpComponentList from '../mcp-component-list/McpComponentList';

const McpServerList = ({mcpServers}: {mcpServers: McpServerType[]}) => {
    return (
        <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
            {mcpServers.map((mcpServer) => {
                return (
                    <Collapsible className="group" defaultOpen={true} key={mcpServer.id}>
                        <McpServerListItem key={mcpServer.id} mcpServer={mcpServer} />

                        <CollapsibleContent>
                            <McpComponentList mcpServer={mcpServer} />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default McpServerList;
