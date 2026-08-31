import Button from '@/components/Button/Button';
import EmptyList from '@/components/EmptyList';
import PageLoader from '@/components/PageLoader';
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow} from '@/components/ui/table';
import AddContextSourceDialog from '@/pages/automation/context-store/components/AddContextSourceDialog';
import ContextStoreFormDialog from '@/pages/automation/context-store/components/ContextStoreFormDialog';
import ContextStoreLeftSidebarNav from '@/pages/automation/context-store/components/ContextStoreLeftSidebarNav';
import ContextStoreSourceDetailDialog from '@/pages/automation/context-store/components/ContextStoreSourceDetailDialog';
import ContextStoreSourceEnabledToggle from '@/pages/automation/context-store/components/ContextStoreSourceEnabledToggle';
import ContextStoreSourceRowActionsMenu from '@/pages/automation/context-store/components/ContextStoreSourceRowActionsMenu';
import useContextStoreSources from '@/pages/automation/context-store/components/hooks/useContextStoreSources';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import SyncSourceStatusBadge from '@/shared/components/SyncSourceStatusBadge';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {AUTHORITIES} from '@/shared/constants';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useContextStoresQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useQueryClient} from '@tanstack/react-query';
import {formatDistanceToNow} from 'date-fns';
import {BoxesIcon, PlusIcon, SparklesIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';

/**
 * Single-store detail page — sources that belong to one Context Store. Mirrors the `KnowledgeBase` detail page: a left
 * sidebar lists every Context Store in the workspace+env so users can hop between them, and the main pane shows the
 * current store's sources.
 */
const ContextStoreSources = () => {
    const {id: contextStoreIdParam} = useParams<{id: string}>();

    const account = useAuthenticationStore((state) => state.account);

    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const copilotEnabled = useApplicationInfoStore((state) => state.ai.copilot.enabled);

    const setContext = useCopilotStore((state) => state.setContext);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const registerPostTurn = useCopilotPostTurnRegistry((state) => state.register);

    const queryClient = useQueryClient();

    const [selectedSourceId, setSelectedSourceId] = useState<string | null>(null);

    const navigate = useNavigate();

    // The environment this detail page was opened in. A context store belongs to one environment, so switching leaves
    // this route pointing at a store the new environment does not contain -- the sidebar empties while the page keeps
    // rendering the old store's sources. Send the reader back to the list rather than showing them that.
    const openedEnvironmentIdRef = useRef(currentEnvironmentId);

    const {error, isLoading, sources} = useContextStoreSources();

    const {data: contextStoresData} = useContextStoresQuery({
        environmentId: String(currentEnvironmentId),
        workspaceId: String(currentWorkspaceId),
    });

    const isAdmin = account?.authorities?.includes(AUTHORITIES.ADMIN) ?? false;

    const contextStore = useMemo(
        () => (contextStoresData?.contextStores ?? []).find((store) => String(store.id) === contextStoreIdParam),
        [contextStoresData?.contextStores, contextStoreIdParam]
    );

    const openCopilot = () => {
        setContext({
            mode: MODE.ASK,
            parameters: {contextStoreId: contextStoreIdParam},
            source: Source.CONTEXT_STORE,
        });

        setCopilotPanelOpen(true);
    };

    useEffect(() => {
        if (openedEnvironmentIdRef.current !== currentEnvironmentId) {
            navigate('/automation/context-stores');
        }
    }, [currentEnvironmentId, navigate]);

    // Refresh this store's sources + the store list after a BUILD-mode copilot turn mutates data (e.g. adding or
    // syncing a source), so the page reflects the change without a manual reload.
    useEffect(() => {
        return registerPostTurn(Source.CONTEXT_STORE, () => {
            queryClient.invalidateQueries({queryKey: ['contextStoreSources']});
            queryClient.invalidateQueries({queryKey: ['contextStores']});
        });
    }, [queryClient, registerPostTurn]);

    // Scope to this store's sources only. The hook returns every source in the workspace+env; filter client-side
    // rather than threading a contextStoreId arg through the GraphQL query — the list is bounded and already
    // cached by the parent route.
    const storeSources = useMemo(
        () => sources.filter((source) => String(source.contextStoreId) === contextStoreIdParam),
        [sources, contextStoreIdParam]
    );

    const storeName = contextStore?.name ?? 'Context Store';

    const selectedSource = useMemo(
        () => storeSources.find((source) => String(source.id) === selectedSourceId) ?? null,
        [storeSources, selectedSourceId]
    );

    return (
        <LayoutContainer
            header={
                <Header
                    centerTitle={true}
                    position="main"
                    right={
                        <div className="flex items-center gap-1">
                            {copilotEnabled && (
                                <Button
                                    aria-label="Ask Copilot"
                                    icon={<SparklesIcon />}
                                    onClick={openCopilot}
                                    size="icon"
                                    variant="ghost"
                                />
                            )}

                            {/* Hidden when there are no sources — the "No Sources" empty state already
                             * renders its own "Add Source" button, so a second one in the header would be
                             * redundant. The header button is only useful once the table view is shown. */}

                            {isAdmin && contextStore && storeSources.length > 0 && (
                                <AddContextSourceDialog
                                    contextStoreId={String(contextStore.id)}
                                    trigger={<Button>Add Source</Button>}
                                />
                            )}
                        </div>
                    }
                    title={storeName}
                />
            }
            leftSidebarBody={<ContextStoreLeftSidebarNav isAdmin={isAdmin} />}
            leftSidebarHeader={
                // The + beside the sidebar title, matching the data tables, knowledge base, skills and agent
                // detail sidebars — creating one from here needs no trip back to the list.
                <Header
                    position="sidebar"
                    right={
                        isAdmin ? (
                            <ContextStoreFormDialog
                                trigger={
                                    <Button
                                        aria-label="New context store"
                                        icon={<PlusIcon />}
                                        size="icon"
                                        variant="ghost"
                                    />
                                }
                            />
                        ) : undefined
                    }
                    title="Context Store"
                />
            }
            leftSidebarWidth="64"
        >
            <PageLoader errors={[error]} loading={isLoading}>
                {storeSources.length > 0 ? (
                    <div className="w-full px-4 py-4">
                        <Table className="w-full">
                            <TableHeader>
                                <TableRow>
                                    <TableHead>Name</TableHead>

                                    <TableHead>Component</TableHead>

                                    <TableHead>Entity</TableHead>

                                    <TableHead>Status</TableHead>

                                    <TableHead>Last Sync</TableHead>

                                    <TableHead className="w-12" />
                                </TableRow>
                            </TableHeader>

                            <TableBody>
                                {storeSources.map((source) => (
                                    <TableRow
                                        className="cursor-pointer [&>td]:py-4"
                                        data-testid={`context-store-source-row-${source.id}`}
                                        key={source.id}
                                        onClick={() => setSelectedSourceId(String(source.id))}
                                    >
                                        <TableCell className="font-medium">{source.name}</TableCell>

                                        <TableCell>{source.sourceComponentName}</TableCell>

                                        <TableCell>{source.entityName}</TableCell>

                                        <TableCell>
                                            <SyncSourceStatusBadge status={source.status} />
                                        </TableCell>

                                        <TableCell className="text-sm text-muted-foreground">
                                            {source.lastSyncRunAt
                                                ? `${formatDistanceToNow(new Date(Number(source.lastSyncRunAt)))} ago`
                                                : 'Never'}
                                        </TableCell>

                                        <TableCell onClick={(event) => event.stopPropagation()}>
                                            <div className="flex items-center justify-end gap-2">
                                                <ContextStoreSourceEnabledToggle isAdmin={isAdmin} source={source} />

                                                <ContextStoreSourceRowActionsMenu isAdmin={isAdmin} source={source} />
                                            </div>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </div>
                ) : (
                    <EmptyList
                        button={
                            isAdmin && contextStore ? (
                                <AddContextSourceDialog
                                    contextStoreId={String(contextStore.id)}
                                    trigger={<Button>Add Source</Button>}
                                />
                            ) : undefined
                        }
                        icon={<BoxesIcon className="size-24 text-stroke-neutral-tertiary" />}
                        message={
                            contextStore
                                ? `Add a source to "${contextStore.name}" to start syncing records into this Context Store.`
                                : 'Get started by adding a Context Store source.'
                        }
                        title="No Sources"
                    />
                )}
            </PageLoader>

            <ContextStoreSourceDetailDialog
                onOpenChange={(open) => {
                    if (!open) {
                        setSelectedSourceId(null);
                    }
                }}
                open={!!selectedSource}
                source={selectedSource}
            />
        </LayoutContainer>
    );
};

export default ContextStoreSources;
