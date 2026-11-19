import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import {McpComponent} from '@/shared/middleware/graphql';
import {ComponentIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

interface McpComponentCollapsiblePropsI {
    badges?: ReactNode;
    children: ReactNode;
    dialog: ReactNode;
    dropdownMenu: ReactNode;
    iconSrc?: string | null;
    mcpComponent: McpComponent;
}

const McpComponentCollapsible = ({
    badges,
    children,
    dialog,
    dropdownMenu,
    iconSrc,
    mcpComponent,
}: McpComponentCollapsiblePropsI) => {
    const [expanded, setExpanded] = useState(false);

    return (
        <Collapsible className="group" onOpenChange={setExpanded} open={expanded}>
            <div className="relative flex items-center gap-2.5 rounded-md py-2.5 pl-3 hover:bg-surface-neutral-primary-hover">
                <ListItemExpandButton
                    className="mr-0 after:absolute after:inset-0"
                    label={expanded ? 'Hide tools' : 'Show tools'}
                />

                {iconSrc ? (
                    <InlineSVG className="size-6 shrink-0" src={iconSrc} />
                ) : (
                    <ComponentIcon className="size-6 shrink-0 text-content-neutral-secondary" />
                )}

                <CollapsibleTrigger asChild>
                    <button className="flex min-w-0 flex-1 cursor-pointer items-center gap-2 text-left" type="button">
                        <span className="truncate text-sm font-medium">
                            {mcpComponent.title || mcpComponent.componentName}
                        </span>
                    </button>
                </CollapsibleTrigger>

                {badges}

                <Tooltip>
                    <TooltipTrigger asChild>
                        <Badge
                            className="relative z-10"
                            label={`v${mcpComponent.componentVersion}`}
                            styleType="outline-outline"
                            weight="semibold"
                        />
                    </TooltipTrigger>

                    <TooltipContent>Component Version</TooltipContent>
                </Tooltip>

                <Tooltip>
                    <TooltipTrigger className="relative z-10 flex min-w-56 items-center justify-end text-xs text-content-neutral-secondary tabular-nums">
                        {mcpComponent.lastModifiedDate
                            ? `Modified at ${new Date(mcpComponent.lastModifiedDate).toLocaleDateString()} ${new Date(mcpComponent.lastModifiedDate).toLocaleTimeString()}`
                            : '-'}
                    </TooltipTrigger>

                    <TooltipContent>Last Updated Date</TooltipContent>
                </Tooltip>

                {dropdownMenu}
            </div>

            <CollapsibleContent>
                <div className="px-3 py-2 pl-10">{children}</div>
            </CollapsibleContent>

            {dialog}
        </Collapsible>
    );
};

export default McpComponentCollapsible;
