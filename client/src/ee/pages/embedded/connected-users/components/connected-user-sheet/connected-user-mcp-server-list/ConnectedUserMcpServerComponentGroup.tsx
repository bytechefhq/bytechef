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
    version,
    versionKind,
}: {
    children: ReactNode;
    componentName: string;
    credentialStatus?: CredentialStatus;
    version: number;
    versionKind: 'component' | 'integration';
}) => {
    const [expanded, setExpanded] = useState(false);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const componentDefinition = componentDefinitions?.find((definition) => definition.name === componentName);

    return (
        <Collapsible className="rounded-md border border-border/50" onOpenChange={setExpanded} open={expanded}>
            <div className="flex items-center gap-2.5 pr-3">
                <CollapsibleTrigger asChild>
                    <button
                        aria-label={expanded ? 'Hide tools' : 'Show tools'}
                        className="flex min-w-0 flex-1 items-center gap-2.5 py-2.5 pl-3 text-left"
                        type="button"
                    >
                        {expanded ? (
                            <ChevronDownIcon className="size-4 shrink-0 text-muted-foreground" />
                        ) : (
                            <ChevronRightIcon className="size-4 shrink-0 text-muted-foreground" />
                        )}

                        {componentDefinition?.icon ? (
                            <InlineSVG className="size-6 shrink-0" src={componentDefinition.icon} />
                        ) : (
                            <ComponentIcon className="size-6 shrink-0 text-content-neutral-secondary" />
                        )}

                        <span className="truncate text-sm font-medium">
                            {componentDefinition?.title || componentName}
                        </span>
                    </button>
                </CollapsibleTrigger>

                <ConnectedUserCredentialStatus
                    componentTitle={componentDefinition?.title || componentName}
                    credentialStatus={credentialStatus}
                />

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
            </div>

            <CollapsibleContent>
                <ul className="flex flex-col gap-1 border-t border-border/50 py-2 pl-10">{children}</ul>
            </CollapsibleContent>
        </Collapsible>
    );
};

export default ConnectedUserMcpServerComponentGroup;
