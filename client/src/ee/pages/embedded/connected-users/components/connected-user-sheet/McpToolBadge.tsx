import Badge from '@/components/Badge/Badge';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';

const McpToolBadge = () => (
    <Tooltip>
        <TooltipTrigger asChild>
            <Badge label="MCP tool" styleType="secondary-filled" />
        </TooltipTrigger>

        <TooltipContent className="max-w-xs">
            An MCP server also exposes this workflow as a tool. This switch turns on the workflow&apos;s other triggers;
            whether the user&apos;s AI agents can call the tool is set on the MCP Servers tab.
        </TooltipContent>
    </Tooltip>
);

export default McpToolBadge;
