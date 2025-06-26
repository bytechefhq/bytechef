import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {McpServer} from '@/shared/middleware/graphql';
import McpServerListItem from 'pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem';

import McpComponentList from '../mcp-component-list/McpComponentList';

const McpServerList = ({mcpServers}: {mcpServers: McpServer[]}) => {
    return (
        <div className="w-full divide-y divide-border/50 px-4 2xl:mx-auto 2xl:w-4/5">
            {mcpServers.map((mcpServer) => {
                return (
                    <Collapsible className="group" key={mcpServer.id}>
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
