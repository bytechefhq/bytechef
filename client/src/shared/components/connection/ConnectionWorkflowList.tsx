import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import OpenInButton from '@/shared/components/OpenInButton';
import {EllipsisVerticalIcon, Link2OffIcon, Loader2Icon, UserIcon} from 'lucide-react';
import {useRef, useState} from 'react';
import {twMerge} from 'tailwind-merge';

export interface ConnectionWorkflowI {
    id: string;
    onOpenClick?: () => void;
    openLabel?: string;
    parentConnectedUser?: boolean;
    parentName: string;
    usage: 'DEPLOYMENT' | 'EDITOR';
    version?: number;
    workflowName: string;
}

interface ConnectionWorkflowListProps {
    connectionWorkflows?: ConnectionWorkflowI[];
    isLoading: boolean;
    onDisconnectClick?: (connectionWorkflow: ConnectionWorkflowI) => void;
}

const TruncatedText = ({className, text}: {className?: string; text: string}) => {
    const [tooltipOpen, setTooltipOpen] = useState(false);

    const textRef = useRef<HTMLSpanElement>(null);

    const handleTooltipOpenChange = (open: boolean) => {
        const element = textRef.current;

        setTooltipOpen(open && !!element && element.scrollWidth > element.clientWidth);
    };

    return (
        <Tooltip onOpenChange={handleTooltipOpenChange} open={tooltipOpen}>
            <TooltipTrigger asChild>
                <span className={twMerge('truncate', className)} ref={textRef}>
                    {text}
                </span>
            </TooltipTrigger>

            <TooltipContent>{text}</TooltipContent>
        </Tooltip>
    );
};

const ConnectionWorkflowListContent = ({
    connectionWorkflows,
    isLoading,
    onDisconnectClick,
}: ConnectionWorkflowListProps) => {
    if (isLoading) {
        return (
            <div className="flex items-center gap-2 py-2 text-sm text-content-neutral-secondary">
                <Loader2Icon className="size-4 animate-spin" />

                <span>Loading workflows...</span>
            </div>
        );
    }

    if (!connectionWorkflows || connectionWorkflows.length === 0) {
        return <p className="py-2 text-sm text-content-neutral-secondary">No workflow uses this connection.</p>;
    }

    return (
        <ul
            className="grid grid-cols-[minmax(0,3fr)_minmax(0,2fr)_9rem] gap-x-6"
            data-testid="connection-workflows-list"
        >
            {connectionWorkflows.map((connectionWorkflow) => (
                <li
                    className="col-span-full grid min-h-11 grid-cols-subgrid items-center border-b border-border/50 py-1 text-sm last:border-b-0"
                    key={`${connectionWorkflow.usage}-${connectionWorkflow.id}`}
                >
                    <div className="flex min-w-0 items-center gap-1.5">
                        <TruncatedText className="font-medium" text={connectionWorkflow.workflowName} />

                        {connectionWorkflow.onOpenClick && (
                            <OpenInButton
                                label={connectionWorkflow.openLabel || 'Open workflow'}
                                onClick={connectionWorkflow.onOpenClick}
                            />
                        )}
                    </div>

                    <div className="flex min-w-0 items-center gap-2">
                        {connectionWorkflow.parentConnectedUser && (
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <UserIcon
                                        aria-label="Connected user"
                                        className="size-4 shrink-0 text-content-neutral-secondary"
                                        role="img"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Connected user</TooltipContent>
                            </Tooltip>
                        )}

                        <TruncatedText
                            className="text-content-neutral-secondary"
                            text={connectionWorkflow.parentName}
                        />

                        {connectionWorkflow.version !== undefined && (
                            <Badge
                                className="shrink-0"
                                label={`V${connectionWorkflow.version}`}
                                styleType="secondary-outline"
                                weight="semibold"
                            />
                        )}
                    </div>

                    <div className="flex items-center justify-end gap-3">
                        <Badge
                            className="border-transparent"
                            label={connectionWorkflow.usage === 'DEPLOYMENT' ? 'Deployed' : 'Editor'}
                            styleType={
                                connectionWorkflow.usage === 'DEPLOYMENT' ? 'primary-outline' : 'secondary-outline'
                            }
                            weight="semibold"
                        />

                        {onDisconnectClick ? (
                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label="Workflow actions"
                                        icon={<EllipsisVerticalIcon />}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                        icon={<Link2OffIcon />}
                                        label="Disconnect"
                                        onClick={() => onDisconnectClick(connectionWorkflow)}
                                    />
                                </DropdownMenuContent>
                            </DropdownMenu>
                        ) : (
                            <div aria-hidden="true" className="size-9 shrink-0" />
                        )}
                    </div>
                </li>
            ))}
        </ul>
    );
};

const ConnectionWorkflowList = (props: ConnectionWorkflowListProps) => (
    <div>
        <h3 className="pb-1 text-sm font-semibold text-content-neutral-tertiary uppercase">Workflows</h3>

        <ConnectionWorkflowListContent {...props} />
    </div>
);

export default ConnectionWorkflowList;
