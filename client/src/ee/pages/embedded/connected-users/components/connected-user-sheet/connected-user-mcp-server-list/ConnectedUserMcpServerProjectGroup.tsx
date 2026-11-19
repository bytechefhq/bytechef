import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ChevronDownIcon, ChevronRightIcon, FolderIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';

const ConnectedUserMcpServerProjectGroup = ({
    children,
    lastExecutionDate,
    projectName,
}: {
    children: ReactNode;
    lastExecutionDate?: Date;
    projectName: string;
}) => {
    const [expanded, setExpanded] = useState(false);

    return (
        <Collapsible
            aria-label={projectName || 'Unknown project'}
            className="rounded-md border border-border/50"
            onOpenChange={setExpanded}
            open={expanded}
            role="group"
        >
            <div className="flex items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="flex min-h-14 min-w-0 flex-1 items-center gap-2.5 py-2 text-left"
                        type="button"
                    >
                        {expanded ? (
                            <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                        ) : (
                            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
                        )}

                        <FolderIcon aria-hidden="true" className="size-5 shrink-0 text-content-neutral-secondary" />

                        <span className="truncate text-sm font-semibold">{projectName || 'Unknown project'}</span>
                    </button>
                </CollapsibleTrigger>

                <div className="flex items-center gap-x-2">
                    <div className="flex w-72 items-center justify-end">
                        <Tooltip>
                            <TooltipTrigger className="text-xs text-content-neutral-secondary">
                                {lastExecutionDate
                                    ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                    : 'No executions'}
                            </TooltipTrigger>

                            <TooltipContent side="left">Last Execution Date</TooltipContent>
                        </Tooltip>
                    </div>

                    <div aria-hidden="true" className="size-9 shrink-0" />
                </div>
            </div>

            <CollapsibleContent>
                <ul className="flex flex-col gap-1 border-t border-border/50 py-2 pl-10">{children}</ul>
            </CollapsibleContent>
        </Collapsible>
    );
};

export default ConnectedUserMcpServerProjectGroup;
