import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ReactNode} from 'react';

import McpServerTabs, {McpServerTabsProps} from './McpServerTabs';

interface McpServerCollapsibleItemProps extends McpServerTabsProps {
    header: ReactNode;
}

const McpServerCollapsibleItem = ({header, ...mcpServerTabsProps}: McpServerCollapsibleItemProps) => (
    <Collapsible className="group mb-2 rounded border border-border/50">
        {header}

        <CollapsibleContent className="mt-1 mr-3 mb-3 ml-11">
            <McpServerTabs {...mcpServerTabsProps} />
        </CollapsibleContent>
    </Collapsible>
);

export default McpServerCollapsibleItem;
