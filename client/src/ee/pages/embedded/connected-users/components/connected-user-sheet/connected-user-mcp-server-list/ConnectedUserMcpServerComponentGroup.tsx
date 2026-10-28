import Badge from '@/components/Badge/Badge';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ConnectedUserCredentialStatus from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserCredentialStatus';
import {CredentialStatus} from '@/ee/shared/middleware/embedded/connected-user';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {ChevronDownIcon, ChevronRightIcon, ComponentIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import InlineSVG from 'react-inlinesvg';

const ConnectedUserMcpServerComponentGroup = ({
    children,
    componentName,
    credentialStatus,
    lastExecutionDate,
    version,
    versionKind,
}: {
    children: ReactNode;
    componentName: string;
    credentialStatus?: CredentialStatus;
    lastExecutionDate?: Date;
    version: number;
    versionKind: 'component' | 'integration';
}) => {
    const [expanded, setExpanded] = useState(false);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentDefinition = componentDefinitions?.find((definition) => definition.name === componentName);

    return (
        <Collapsible className="rounded-md border border-border/50" onOpenChange={setExpanded} open={expanded}>
            <div className="flex items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="flex min-w-0 flex-1 flex-col items-start gap-y-1 py-2 text-left"
                        type="button"
                    >
                        <span className="flex min-w-0 items-center gap-2.5">
                            {expanded ? (
                                <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                            ) : (
                                <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
                            )}

                            {componentDefinition?.icon ? (
                                <InlineSVG className="size-5 shrink-0" src={componentDefinition.icon} />
                            ) : (
                                <ComponentIcon className="size-5 shrink-0 text-content-neutral-secondary" />
                            )}

                            <span className="truncate text-sm font-semibold">
                                {componentDefinition?.title || componentName}
                            </span>
                        </span>

                        <span className="flex items-center pl-6.5 text-xs">
                            <ConnectedUserCredentialStatus
                                componentTitle={componentDefinition?.title || componentName}
                                credentialStatus={credentialStatus}
                            />
                        </span>
                    </button>
                </CollapsibleTrigger>

                <div className="flex items-center gap-x-2">
                    <div className="flex items-center gap-x-4">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Badge
                                    label={`v${version}`}
                                    styleType={versionKind === 'component' ? 'outline-outline' : 'secondary-filled'}
                                    weight="semibold"
                                />
                            </TooltipTrigger>

                            <TooltipContent>
                                {versionKind === 'component' ? 'Component Version' : 'Integration Version'}
                            </TooltipContent>
                        </Tooltip>

                        <div className="flex min-w-52 items-center justify-end">
                            {versionKind === 'integration' && (
                                <Tooltip>
                                    <TooltipTrigger className="text-xs text-content-neutral-secondary">
                                        {lastExecutionDate
                                            ? `Executed at ${lastExecutionDate.toLocaleDateString()} ${lastExecutionDate.toLocaleTimeString()}`
                                            : 'No executions'}
                                    </TooltipTrigger>

                                    <TooltipContent side="left">Last Execution Date</TooltipContent>
                                </Tooltip>
                            )}
                        </div>
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

export default ConnectedUserMcpServerComponentGroup;
