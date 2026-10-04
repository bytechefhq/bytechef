import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {ReactNode} from 'react';

import McpServerTabs, {McpServerTabsProps} from './McpServerTabs';

interface McpServerCollapsibleItemProps extends McpServerTabsProps {
    header: ReactNode;
}

const McpServerCollapsibleItem = ({header, ...mcpServerTabsProps}: McpServerCollapsibleItemProps) => (
    <Collapsible className="group mb-2 rounded border border-border/50">
        {header}

        <CollapsibleContent className="mx-3 mt-1 mb-3">
            <McpServerTabs {...mcpServerTabsProps} />
        </CollapsibleContent>
    </Collapsible>
);

export default McpServerCollapsibleItem;
