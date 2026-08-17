import Switch from '@/components/Switch/Switch';
import {CommandGroup, CommandItem} from '@/components/ui/command';
import {useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {
    AiHubUserConnectorsQuery,
    useAiHubUserConnectorsQuery,
    useSetAiHubChatConnectorEnabledMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronLeftIcon, Loader2Icon, PlugIcon, SettingsIcon} from 'lucide-react';
import {useMemo} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

type UserConnectorType = AiHubUserConnectorsQuery['aiHubUserConnectors'][number];

interface AiHubConnectorsBranchPropsI {
    onBack: () => void;
    onClose: () => void;
}

/**
 * Connectors branch of the composer's "+" Resources menu. Lists the connectors available to the user, each with
 * a switch controlling whether it acts in THIS chat, plus a "Manage connectors" link.
 *
 * This was a standalone toolbar button (the plug icon) until the composer's pickers were folded into one menu.
 * The query needs no `enabled` gate any more: the branch mounts only once the user drills into it, which gates
 * the fetch more tightly than the old popover-open flag did.
 *
 * Two scopes, deliberately split across two surfaces:
 *  - Connectors page → AVAILABILITY. A connector disabled there is unusable everywhere, and is filtered out of
 *    this list entirely rather than shown as an un-flippable row.
 *  - This switch → PARTICIPATION. It writes a chat-scoped record and leaves availability alone, so turning a
 *    connector off mid-conversation no longer silently changes every other chat (which is what the old
 *    setAiHubUserConnectorEnabled call from here did).
 *
 * On the home composer there is no chat yet, so the switches are absent and the list reads as availability
 * only; participation becomes editable once the first turn creates the chat.
 */
const AiHubConnectorsBranch = ({onBack, onClose}: AiHubConnectorsBranchPropsI) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const chatId = useAiHubStore((state) => state.chatId);

    const queryClient = useQueryClient();

    const {data, isLoading} = useAiHubUserConnectorsQuery(
        {chatId, workspaceId: String(currentWorkspaceId ?? '')},
        {enabled: currentWorkspaceId != null}
    );

    const setEnabledInChatMutation = useSetAiHubChatConnectorEnabledMutation({
        onSuccess: () => queryClient.invalidateQueries({queryKey: ['aiHubUserConnectors']}),
    });

    const connectors = useMemo(
        () =>
            [...(data?.aiHubUserConnectors ?? [])]
                .filter((connector) => connector.enabled)
                .sort((first, second) =>
                    (first.title ?? first.componentName).localeCompare(second.title ?? second.componentName)
                ),
        [data]
    );

    const handleToggle = (connector: UserConnectorType, enabledInChat: boolean) => {
        if (chatId == null) {
            return;
        }

        setEnabledInChatMutation.mutate({
            chatId,
            connectorId: connector.id,
            enabled: enabledInChat,
            workspaceId: String(currentWorkspaceId ?? ''),
        });
    };

    return (
        <>
            <CommandGroup>
                <CommandItem onSelect={onBack} value="back-to-root">
                    <ChevronLeftIcon className="mr-2 size-3.5" />

                    <span className="flex-1 text-muted-foreground">Back</span>
                </CommandItem>
            </CommandGroup>

            {/* Plain rows rather than CommandItems: each carries a Switch, and a CommandItem's onSelect would
             * fire on the same click that flips it. */}
            <div className="max-h-80 overflow-y-auto p-1">
                {isLoading && (
                    <div className="flex items-center justify-center gap-2 px-2 py-4 text-sm text-muted-foreground">
                        <Loader2Icon className="size-4 animate-spin" />

                        <span>Loading…</span>
                    </div>
                )}

                {!isLoading && connectors.length === 0 && (
                    <div className="px-2 py-4 text-center text-sm text-muted-foreground">No connectors available.</div>
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

                        {chatId != null && (
                            <Switch
                                checked={connector.enabledInChat}
                                onCheckedChange={(checked) => handleToggle(connector, checked)}
                            />
                        )}
                    </div>
                ))}
            </div>

            <div className="border-t border-border p-1">
                <Link
                    className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm text-muted-foreground hover:bg-accent hover:text-foreground"
                    onClick={onClose}
                    to="/automation/ai-hub/context/connectors"
                >
                    <SettingsIcon className="size-4 shrink-0" />

                    <span>Manage connectors</span>
                </Link>
            </div>
        </>
    );
};

export default AiHubConnectorsBranch;
