import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import AiHubChatsSidebarToggle from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarToggle';
import AiHubErrorBoundary from '@/ee/pages/automation/ai-hub/AiHubErrorBoundary';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import AiHubChatsSidebar from '@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import useRecordReferencedArtifacts from '@/ee/pages/automation/ai-hub/chats/hooks/useRecordReferencedArtifacts';
import {useSwitchChat} from '@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {aiHubChatsStore, useAiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {useResetAiHubChatOnEnvironmentChange} from '@/ee/pages/automation/ai-hub/hooks/useResetAiHubChatOnEnvironmentChange';
import {useResetAiHubStoresOnWorkspaceChange} from '@/ee/pages/automation/ai-hub/hooks/useResetAiHubStoresOnWorkspaceChange';
import {AiHubRuntimeProvider} from '@/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {MessageSquareOffIcon} from 'lucide-react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {type PanelImperativeHandle} from 'react-resizable-panels';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const RESOURCE_PANEL_ANIMATION_MS = 320;

const AiHubChatLoading = () => (
    <div className="flex size-full items-center justify-center p-8" data-testid="ai-hub-chat-loading">
        <div className="flex animate-pulse space-x-2">
            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

            <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>
        </div>
    </div>
);

const AiHubChatNotFound = () => (
    <div className="flex size-full flex-col">
        <div className="flex items-center p-4">
            <div className="flex h-header-height items-center">
                <AiHubChatsSidebarToggle />
            </div>
        </div>

        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-4 text-center">
            <MessageSquareOffIcon className="size-10 text-content-neutral-secondary" />

            <div className="flex flex-col gap-1">
                <p className="text-sm font-medium text-content-neutral-primary">Chat not found</p>

                <p className="text-sm text-content-neutral-secondary">
                    This chat does not exist, or you do not have access to it.
                </p>
            </div>

            <Link
                className="inline-flex h-9 items-center rounded-md bg-surface-brand-primary px-4 text-sm font-medium text-content-onsurface-primary hover:bg-surface-brand-primary-hover"
                to="/automation/ai-hub"
            >
                Start a new chat
            </Link>
        </div>
    </div>
);

/**
 * The AI Hub page: the chats sidebar, the home view or the active chat, and the collapsible workflow resource panel.
 * The runtime provider and its error boundary sit above the home / chat swap, so the first message's turn survives the
 * switch from the home view to the chat view.
 *
 * <p>The route's chat id and the chats store are kept in sync by one effect: a store change (row click, chat
 * creation, delete) pushes the URL first; otherwise a URL chat id switches the store once the active or archived chats
 * list holds it; a
 * URL with no chat id resets the store on a transition home or on mount. A URL chat id neither list holds (another
 * user's chat, a deleted one, a refused request) shows the not-found state.</p>
 */
const AiHub = () => {
    const [resourcePanelMountedState, setResourcePanelMountedState] = useState(false);

    const chatPaneElementRef = useRef<HTMLDivElement | null>(null);
    const isFirstEffectRef = useRef(true);
    const previousStoreIdRef = useRef<number | undefined>(undefined);
    const previousUrlParamRef = useRef<string | undefined>(undefined);
    const resourcePaneElementRef = useRef<HTMLDivElement | null>(null);
    const resourcePanelHandleRef = useRef<PanelImperativeHandle | null>(null);

    const generateChatId = useAiHubStore((state) => state.generateChatId);
    const hasMessages = useAiHubStore((state) => state.messages.length > 0);
    const resetMessages = useAiHubStore((state) => state.resetMessages);
    const setMode = useAiHubStore((state) => state.setMode);
    const chatsSidebarCollapsed = useAiHubTabsStore((state) => state.chatsSidebarCollapsed);
    const hasOpenTabs = useAiHubTabsStore((state) => state.openTabs.length > 0);
    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const setActiveTabsChatId = useAiHubTabsStore((state) => state.setActiveChatId);
    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const navigate = useNavigate();
    const {chatId: urlChatIdParam} = useParams<{chatId?: string}>();

    const {data: activeChats, isLoading: activeChatsLoading} = useAiHubChatsQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        'ACTIVE'
    );
    const {data: archivedChats, isLoading: archivedChatsLoading} = useAiHubChatsQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        'ARCHIVED'
    );

    const switchChat = useSwitchChat();

    useResetAiHubStoresOnWorkspaceChange();
    useResetAiHubChatOnEnvironmentChange();
    useRecordReferencedArtifacts(currentChatId, currentWorkspaceId);

    const hasActiveChat = currentChatId != null || hasMessages;
    const hasResourceContent = hasActiveChat || hasOpenTabs;
    const showResourcePanel = rightPanelOpen && hasResourceContent;
    const resourcePanelMounted = hasResourceContent || resourcePanelMountedState;

    const chats = useMemo(() => [...(activeChats ?? []), ...(archivedChats ?? [])], [activeChats, archivedChats]);

    const chatsLoading = !isWorkspaceIdKnown(currentWorkspaceId) || activeChatsLoading || archivedChatsLoading;

    const urlChatId = urlChatIdParam ? Number(urlChatIdParam) : undefined;
    const isUrlChatKnown = chats.some((chat) => chat.id === urlChatId);
    const isUrlChatCurrent = urlChatId != null && urlChatId === currentChatId;

    const chatNotFound =
        !!urlChatIdParam && !isUrlChatCurrent && (Number.isNaN(urlChatId) || (!chatsLoading && !isUrlChatKnown));

    const pendingUrlChat = !hasActiveChat && !!urlChatIdParam && !chatNotFound && (chatsLoading || isUrlChatKnown);

    useEffect(() => {
        setMode(MODE.BUILD);
    }, [setMode]);

    useEffect(() => {
        setActiveTabsChatId(currentChatId);
    }, [currentChatId, setActiveTabsChatId]);

    useEffect(() => {
        aiChatAskedQuestionsStore.getState().reset();
    }, [currentChatId]);

    useEffect(() => {
        const panel = resourcePanelHandleRef.current;

        if (panel == null) {
            return;
        }

        const chatElement = chatPaneElementRef.current;
        const resourceElement = resourcePaneElementRef.current;

        if (chatElement != null) {
            chatElement.style.transition = 'flex 300ms ease-in-out';
        }

        if (resourceElement != null) {
            resourceElement.style.transition = 'flex 300ms ease-in-out';
        }

        if (showResourcePanel) {
            if (panel.isCollapsed()) {
                panel.expand();
            }

            panel.resize('62');
        } else {
            panel.collapse();
        }

        const timerId = setTimeout(() => {
            if (chatElement != null) {
                chatElement.style.transition = '';
            }

            if (resourceElement != null) {
                resourceElement.style.transition = '';
            }
        }, RESOURCE_PANEL_ANIMATION_MS);

        return () => clearTimeout(timerId);
    }, [showResourcePanel]);

    useEffect(() => {
        if (hasResourceContent) {
            setResourcePanelMountedState(true);

            return;
        }

        const timerId = setTimeout(() => setResourcePanelMountedState(false), RESOURCE_PANEL_ANIMATION_MS);

        return () => clearTimeout(timerId);
    }, [hasResourceContent]);

    useEffect(() => {
        if (isFirstEffectRef.current) {
            previousStoreIdRef.current = currentChatId;
            previousUrlParamRef.current = urlChatIdParam;
        }

        const urlChanged = urlChatIdParam !== previousUrlParamRef.current;
        const storeChanged = currentChatId !== previousStoreIdRef.current;
        const isFirstEffect = isFirstEffectRef.current;

        previousUrlParamRef.current = urlChatIdParam;
        previousStoreIdRef.current = currentChatId;
        isFirstEffectRef.current = false;

        if (storeChanged) {
            if (currentChatId != null && String(currentChatId) !== urlChatIdParam) {
                navigate(`/automation/ai-hub/chats/${currentChatId}`);
            } else if (currentChatId == null && urlChatIdParam) {
                navigate('/automation/ai-hub');
            }

            return;
        }

        if (urlChatIdParam) {
            const idNumber = Number(urlChatIdParam);

            if (!Number.isNaN(idNumber) && idNumber !== currentChatId) {
                const chat = chats.find((candidate) => candidate.id === idNumber);

                if (chat) {
                    void switchChat(chat);
                }
            }

            return;
        }

        if ((urlChanged || isFirstEffect) && currentChatId != null) {
            aiHubChatsStore.getState().setCurrentChatId(undefined);

            resetMessages();
            generateChatId();
        }
    }, [chats, currentChatId, generateChatId, navigate, resetMessages, switchChat, urlChatIdParam]);

    let mainBody;

    if (chatNotFound) {
        mainBody = <AiHubChatNotFound />;
    } else if (pendingUrlChat) {
        mainBody = <AiHubChatLoading />;
    } else {
        mainBody = (
            <div className="flex size-full">
                <ResizablePanelGroup className="min-w-0 flex-1" orientation="horizontal">
                    <ResizablePanel elementRef={chatPaneElementRef} minSize="25%">
                        {hasActiveChat ? <AiHubPanel /> : <AiHubHomePanel />}
                    </ResizablePanel>

                    <ResizableHandle
                        className={twMerge('w-1 bg-transparent', !showResourcePanel && 'pointer-events-none opacity-0')}
                    />

                    <ResizablePanel
                        collapsedSize="0%"
                        collapsible
                        defaultSize="0%"
                        elementRef={resourcePaneElementRef}
                        minSize="30%"
                        panelRef={resourcePanelHandleRef}
                    >
                        <div className="size-full overflow-hidden">
                            {resourcePanelMounted && <AiHubResourcePanel />}
                        </div>
                    </ResizablePanel>
                </ResizablePanelGroup>
            </div>
        );
    }

    return (
        <LayoutContainer
            className="bg-surface-main"
            leftSidebarBody={<AiHubChatsSidebar />}
            leftSidebarHeader={<Header position="sidebar" title="AI Hub" />}
            leftSidebarOpen={!chatsSidebarCollapsed}
            leftSidebarWidth="64"
        >
            <AiHubErrorBoundary open={true}>
                <AiHubRuntimeProvider>{mainBody}</AiHubRuntimeProvider>
            </AiHubErrorBoundary>
        </LayoutContainer>
    );
};

export default AiHub;
