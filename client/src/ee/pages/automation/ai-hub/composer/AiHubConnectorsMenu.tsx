import Switch from '@/components/Switch/Switch';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiHubUserConnectorsQuery,
    useAiHubUserConnectorsQuery,
    useSetAiHubUserConnectorEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {Loader2Icon, PlugIcon, SettingsIcon} from 'lucide-react';
import {useMemo, useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

type UserConnectorType = AiHubUserConnectorsQuery['aiHubUserConnectors'][number];

/**
 * Composer "connectors" affordance (the button after the "+" resource picker). Opens a popover listing the
 * user's ADDED connectors (same model as the Context > Connectors page), each with a quick on/off toggle, plus
 * a "Manage connectors" link to add more / configure tools.
 *
 * The toggle flips the per-user connector's enabled flag (setAiHubUserConnectorEnabled): off removes its tools
 * from the agent for THIS user. The query is gated on the popover being open so we don't fetch until needed.
 */
const AiHubConnectorsMenu = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const [open, setOpen] = useState(false);

    const queryClient = useQueryClient();

    const {data, isLoading} = useAiHubUserConnectorsQuery(
        {workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: open && currentWorkspaceId != null}
    );

    const setEnabledMutation = useSetAiHubUserConnectorEnabledMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']}),
    });

    const connectors = useMemo(
        () =>
            [...(data?.aiHubUserConnectors ?? [])].sort((first, second) =>
                (first.title ?? first.componentName).localeCompare(second.title ?? second.componentName)
            ),
        [data]
    );

    const handleToggle = (connector: UserConnectorType, enabled: boolean) => {
        setEnabledMutation.mutate({
            connectorId: connector.id,
            enabled,
            workspaceId: String(currentWorkspaceId ?? ''),
        });
    };

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <Tooltip>
                <TooltipTrigger asChild>
                    <PopoverTrigger asChild>
                        <button
                            aria-label="Connectors"
                            className="flex size-7 items-center justify-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground"
                            type="button"
                        >
                            <PlugIcon className="size-4" />
                        </button>
                    </PopoverTrigger>
                </TooltipTrigger>

                <TooltipContent>Connectors</TooltipContent>
            </Tooltip>

            <PopoverContent align="start" className="w-72 p-0" side="top">
                <div className="max-h-80 overflow-y-auto p-1">
                    {isLoading && (
                        <div className="flex items-center justify-center gap-2 px-2 py-4 text-sm text-muted-foreground">
                            <Loader2Icon className="size-4 animate-spin" />

                            <span>Loading…</span>
                        </div>
                    )}

                    {!isLoading && connectors.length === 0 && (
                        <div className="px-2 py-4 text-center text-sm text-muted-foreground">
                            No connectors added yet.
                        </div>
                    )}

                    {connectors.map((connector) => (
                        <div className="flex items-center gap-2 rounded-md px-2 py-1.5" key={connector.id}>
                            {connector.icon ? (
                                <InlineSVG className="size-5 shrink-0" src={connector.icon} />
                            ) : (
                                <PlugIcon className="size-5 shrink-0 text-muted-foreground" />
                            )}

                            <span className="min-w-0 flex-1 truncate text-sm" title={connector.title ?? undefined}>
                                {connector.title ?? connector.componentName}
                            </span>

                            <Switch
                                checked={connector.enabled}
                                onCheckedChange={(checked) => handleToggle(connector, checked)}
                            />
                        </div>
                    ))}
                </div>

                <div className="border-t border-border p-1">
                    <Link
                        className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                        onClick={() => setOpen(false)}
                        to="/automation/ai-hub/context/connectors"
                    >
                        <SettingsIcon className="size-4 shrink-0" />

                        <span>Manage connectors</span>
                    </Link>
                </div>
            </PopoverContent>
        </Popover>
    );
};

export default AiHubConnectorsMenu;
