import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import AiHubChatsSidebarPinButton from '@/ee/pages/automation/ai-hub/AiHubChatsSidebarPinButton';
import AiHubErrorBoundary from '@/ee/pages/automation/ai-hub/AiHubErrorBoundary';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import AiHubChatsSidebar from '@/ee/pages/automation/ai-hub/chats/AiHubChatsSidebar';
import {isWebhookBridgedChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {useAiHubChatsQuery} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import useRecordReferencedArtifacts from '@/ee/pages/automation/ai-hub/chats/hooks/useRecordReferencedArtifacts';
import {useSwitchChat} from '@/ee/pages/automation/ai-hub/chats/hooks/useSwitchChat';
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
import {useEffect, useRef, useState} from 'react';
import {type PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const AiHubContent = () => {
    const setMode = useAiHubStore((state) => state.setMode);
    const generateChatId = useAiHubStore((state) => state.generateChatId);
    const resetMessages = useAiHubStore((state) => state.resetMessages);

    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const chatsSidebarCollapsed = useAiHubTabsStore((state) => state.chatsSidebarCollapsed);
    const chatsSidebarPeeking = useAiHubTabsStore((state) => state.chatsSidebarPeeking);
    const setActiveTabsChatId = useAiHubTabsStore((state) => state.setActiveChatId);
    const setChatsSidebarPeeking = useAiHubTabsStore((state) => state.setChatsSidebarPeeking);

    // The Chats sidebar is a page-level control, toggled by the PanelLeftIcon button that sits before the
    // title in the panel header (AiHubChatsSidebarToggle) — same as the sidebar toggle on every other
    // page. It's independent of the resource (right) panel: opening the resource panel no longer
    // collapses the sidebar, because the toggle is always there to close it by hand.
    const showChatsSidebar = !chatsSidebarCollapsed;

    // While hidden, resting the pointer on the toggle "peeks" the sidebar: it floats in OVER the content
    // as an overlay (no reflow) and slides back out when the pointer leaves it. The peek pops in
    // instantly (`leftSidebarAnimate` off) so it lands under the resting pointer, whereas docking /
    // hiding it and the peek's slide-out are animated like any other page's sidebar.
    const peekChatsSidebar = chatsSidebarCollapsed && chatsSidebarPeeking;

    const currentChatId = useAiHubChatsStore((state) => state.currentChatId);
    // Flip to the thread view as soon as there are messages, not only once the DB chat id arrives. On the
    // home→chat transition, AiHubRuntimeProvider.onNew adds the user message before awaiting the (possibly
    // slow) chat creation — gating on messages here makes that switch feel instant instead of leaving the
    // user on a frozen home page. The selector returns a boolean so it only re-renders on the 0↔non-empty
    // edge, not on every streamed chunk.
    const hasMessages = useAiHubStore((state) => state.messages.length > 0);
    const hasActiveChat = currentChatId != null || hasMessages;

    const hasOpenTabs = useAiHubTabsStore((state) => state.openTabs.length > 0);

    // Named so the two places that need it (the visibility gate below and the delayed-unmount effect
    // further down) can't drift apart.
    const hasResourceContent = hasActiveChat || hasOpenTabs;

    // The resource (right) panel shows for an active chat AND on the home view the moment a composer
    // pick opens a tab — "if I add a resource, I should see it immediately", before the first message
    // creates the chat. The home→chat snapshot carry-over in setActiveChatId keeps those tabs.
    const showResourcePanel = rightPanelOpen && hasResourceContent;

    // Coordinated open/close for the RIGHT resource panel (the left sidebar behavior is untouched).
    // The resource pane is a COLLAPSIBLE panel that's always mounted (when a chat is active) so its width
    // can animate from 0 — rather than the chat snapping to 35% the instant a pre-sized split mounts.
    //
    // `react-resizable-panels` v4 applies `className` to a nested div, not the flex item, so a CSS
    // `transition` in className can't animate the panel's flex. Instead we grab each pane's ROOT element
    // via `elementRef` and toggle a `flex` transition imperatively — enabled only for the ~300ms around a
    // programmatic open/close, then removed so dragging the handle stays snappy (a standing transition
    // would lag against the library's per-frame flex writes during a drag).
    const resourcePanelHandleRef = useRef<PanelImperativeHandle | null>(null);
    const chatPaneElementRef = useRef<HTMLDivElement | null>(null);
    const resourcePaneElementRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const panel = resourcePanelHandleRef.current;

        if (panel == null) {
            return;
        }

        const chatElement = chatPaneElementRef.current;
        const resourceElement = resourcePaneElementRef.current;

        // Animate BOTH panes' flex for open and close. On close this is what makes the chat actually
        // slide: `collapse()` only zeroes the resource's flex, so without a transition on the resource the
        // chat would snap to full width. Animating the resource 62%→0 lets the chat grow into it smoothly
        // (slides to the right); on open it narrows from 100% as the resource grows from 0.
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

            // v4: a NUMBER is pixels, a STRING (no unit) is a percentage. '62' = 62% of the group; passing
            // 62 would open the pane to a 62px sliver.
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
        }, 320);

        return () => clearTimeout(timerId);
    }, [showResourcePanel]);

    // Mirrors `hasResourceContent`, but on the falling edge (last tab closed / chat cleared) it lags
    // behind by the same 320ms as the collapse animation above instead of flipping synchronously. Without
    // this lag, `AiHubResourcePanel` would unmount in the SAME commit that `hasResourceContent` goes
    // false — before the width-collapse effect (keyed on `showResourcePanel`, which reads this same
    // predicate) has even had a chance to run — leaving a frame where the pane is still ~62% wide but
    // visually empty, then narrows to 0. Rising edge (content appearing) stays synchronous: the pane
    // should show its content immediately, same as it always expanded immediately.
    const [resourcePanelMounted, setResourcePanelMounted] = useState(hasResourceContent);

    useEffect(() => {
        if (hasResourceContent) {
            setResourcePanelMounted(true);

            return;
        }

        const timerId = setTimeout(() => setResourcePanelMounted(false), 320);

        return () => clearTimeout(timerId);
    }, [hasResourceContent]);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {data: chats, isLoading: chatsLoading} = useAiHubChatsQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        'ACTIVE'
    );

    const navigate = useNavigate();
    const {chatId: urlChatIdParam} = useParams<{chatId?: string}>();
    const switchChat = useSwitchChat();

    // Deep link to /chats/:id lands here with an empty store: the URL→store sync effect below can only
    // switch to the chat once the (async) chats query resolves. Without this flag the home panel would
    // flash in that gap — hold the loader instead, but only while the id can still resolve (query in
    // flight, or the chat exists in the resolved list). An unknown/foreign id falls back to the home
    // view, matching the sync effect's silent no-op for missing chats.
    const pendingUrlChat =
        !hasActiveChat &&
        !!urlChatIdParam &&
        (chatsLoading || (chats?.some((chat) => String(chat.id) === urlChatIdParam) ?? false));

    // Active chat lookup happens inside AiHubPanel now — title + workflow-chat badge
    // both live there alongside the Ask/Build toggle. The CC root no longer needs the title because the
    // top header strip is gone.

    // Hoisted to this level so the workspace-store reset effect runs once for the whole AI Hub
    // surface — children (HomePanel, Panel, ResourcePanel) used to register it independently, which fired
    // the reset multiple times on workspace change.
    useResetAiHubStoresOnWorkspaceChange();

    useResetAiHubChatOnEnvironmentChange();

    // Records each open right-panel tab as a `ai_hub_chat_artifact` so it shows up in the sidebar
    // artifact list. Bridges the gap between the UI-only tab state and the persistent artifact log —
    // covers BOTH user-driven attachment (composer plus-button → opens tab) and agent-driven attachment
    // (chat tool calls that open files/workflows). The hook is a no-op when there's no active
    // chat (home view); it resumes recording once a chat is created.
    //
    // Disabled for WORKFLOW_CHAT chats: they auto-open their bound workflow in the right panel, but a
    // workflow chat routes through a webhook (not the LLM) and must not accrue artifacts — otherwise that
    // auto-opened workflow would be recorded as a (spurious) artifact of the chat.
    const currentChatIsWorkflowChat = isWebhookBridgedChat(chats?.find((chat) => chat.id === currentChatId)?.kind);

    useRecordReferencedArtifacts(currentChatId, currentWorkspaceId ?? 0, !currentChatIsWorkflowChat);

    useEffect(() => {
        setMode(MODE.BUILD);
    }, [setMode]);

    /*
     * Mirror the active chat into the tabs store so per-chat tab snapshots can be
     * saved/restored across switches. Without this wiring the resource-panel tabs would persist
     * VISUALLY across chat changes (the store is global) but they wouldn't be associated with
     * any chat — switching from /chats/A back to /chats/B would still show
     * chat A's tabs because the store doesn't know which chat it's mirroring.
     */
    useEffect(() => {
        setActiveTabsChatId(currentChatId);
    }, [currentChatId, setActiveTabsChatId]);

    /*
     * Reset the askUserQuestion answered-state store on chat switch. The store is keyed by question
     * content fingerprint (question text + option labels), which is content-stable per question instance
     * but NOT scoped to a chat. Two different chats producing the same question shape (e.g. "Which Slack
     * channel?" with the same channel list) would otherwise share the answered state — chat B would see
     * chat A's answer as already-submitted, hide the buttons, and confuse the user. The workspace-change
     * reset hook already calls reset() for cross-workspace switches; this effect covers in-workspace
     * chat switches.
     */
    useEffect(() => {
        aiChatAskedQuestionsStore.getState().reset();
    }, [currentChatId]);

    // Note: workflow chats intentionally do NOT auto-open their bound workflow in the right resource
    // panel. A workflow chat is a chat — popping the full workflow editor (Publish/Deploy, editable nodes)
    // on every open was heavy and surprising. The workflow can still be opened manually via the right
    // panel's resource picker if the user wants to inspect it.

    /*
     * URL <-> store sync — single effect with explicit priority ordering.
     *
     * Three flow shapes need to coexist without cross-firing:
     *
     *   (a) STORE-DRIVEN — `storeChanged` is true. Caused by a sidebar chat row click
     *       (`switchChat` writes the store), the auto-create path inside `onNew`
     *       (`createAiHubChat` then `setCurrentChatId`), or an explicit reset (delete /
     *       archive of the active chat). The URL must catch up. ALWAYS HANDLED FIRST so the
     *       URL→store invariant below cannot revert the store back to whatever the URL still says.
     *
     *   (b) URL-DRIVEN STANDING — URL has a chat id and the store doesn't yet match. Triggered
     *       on cold-mount deep links and on every render until the chats query resolves. This
     *       has to be a STANDING condition (not gated on cross-render diff) because the chats
     *       list is async and may arrive after the first render — gating on `urlChanged` made the
     *       deep-link case silently no-op whenever the data landed on render 2+.
     *
     *   (c) URL → home — URL has no chat id but the store still does. Reset the store.
     *       Fires under TWO conditions:
     *         1. URL just transitioned to home (`urlChanged`) — sidebar AI Hub icon click.
     *         2. Initial effect run on mount (`isFirstEffectRef`) — catches the remount case where
     *            the user clicks a chat, navigates to another page, then returns to
     *            /ai-hub with `urlChatIdParam = undefined` and a stale
     *            `currentChatId` left in the global store from before navigation.
     *       NOT a fully-standing condition: between branch (a) calling `navigate()` and React Router's
     *       URL context catching up, there's a render where store has the new id but `useParams`
     *       still returns `undefined`. A standing branch (c) would erroneously reset the store there,
     *       triggering an A → C → A bounce loop (URL flips home → chat → home → chat
     *       on every send-from-home flow).
     *
     * The previous shape ran (b) before (a). When the user clicked a sibling chat row from
     * /chats/17 to /chats/18, the store changed to 18 but the URL was still "17"; (b)
     * saw the mismatch, found chat 17 in the list, and called `switchChat(17)`,
     * which clobbered the user's intent. Symptom: clicking a different chat appeared to do
     * nothing (the panel stayed on 17). The order swap below fixes that without breaking deep links.
     */
    const previousUrlParamRef = useRef(urlChatIdParam);
    const previousStoreIdRef = useRef(currentChatId);
    const isFirstEffectRef = useRef(true);

    useEffect(() => {
        const urlChanged = urlChatIdParam !== previousUrlParamRef.current;
        const storeChanged = currentChatId !== previousStoreIdRef.current;
        const isFirstEffect = isFirstEffectRef.current;

        previousUrlParamRef.current = urlChatIdParam;
        previousStoreIdRef.current = currentChatId;
        isFirstEffectRef.current = false;

        // (a) STORE-DRIVEN: highest priority. Push URL to match the store.
        if (storeChanged) {
            if (currentChatId != null && String(currentChatId) !== urlChatIdParam) {
                navigate(`/automation/ai-hub/chats/${currentChatId}`);
            } else if (currentChatId == null && urlChatIdParam) {
                navigate('/automation/ai-hub');
            }

            return;
        }

        // (b) URL-DRIVEN STANDING: URL has an id, keep store aligned. Self-heals across renders.
        if (urlChatIdParam) {
            const idNum = Number(urlChatIdParam);

            if (!Number.isNaN(idNum) && idNum !== currentChatId) {
                const chat = chats?.find((candidate) => candidate.id === idNum);

                if (chat) {
                    void switchChat(chat);
                }
                // No chat found: fall through silently. Next chats refetch retries.
            }

            return;
        }

        // (c) URL → home. Reset store on (1) URL transition to home or (2) first effect run after mount
        // with a stale store value. NOT on intermediate renders during branch (a)'s navigate() flush.
        if ((urlChanged || isFirstEffect) && currentChatId != null) {
            aiHubChatsStore.getState().setCurrentChatId(undefined);
            resetMessages();
            generateChatId();
        }
    }, [chats, currentChatId, generateChatId, navigate, resetMessages, switchChat, urlChatIdParam]);

    // Home view: no active chat yet — show only the centered composer. The first message sent here
    // auto-creates a chat (see AiHubRuntimeProvider.onNew), which flips this branch to the
    // panel view containing the thread.
    // While a deep-linked chat is still resolving, render the same four-dot pulse as
    // LazyLoadWrapper's suspense fallback so the route-chunk load and the chat resolution read as one
    // continuous loading state instead of loader → home flash → chat.
    const mainBody = pendingUrlChat ? (
        <div className="flex size-full items-center justify-center p-8">
            <div className="flex animate-pulse space-x-2">
                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>
            </div>
        </div>
    ) : (
        <div className="flex size-full">
            <ResizablePanelGroup className="min-w-0 flex-1" orientation="horizontal">
                <ResizablePanel elementRef={chatPaneElementRef} minSize="25%">
                    {hasActiveChat ? <AiHubPanel /> : <AiHubHomePanel />}
                </ResizablePanel>

                {/* Invisible draggable gap (no grip line) so the resource panel reads as a floating
                 * "island". This transparent 4px separator is the left gutter between the chat and the
                 * card — deliberately tighter than the `py-2 pr-2` (8px) gutter the panel paints on its
                 * other three sides: the card already reads as detached via its border + shadow, so the
                 * inter-panel gap can be smaller than the window-edge gutters without losing the island look.
                 * Hidden + non-interactive while the resource panel is collapsed (nothing to drag against a
                 * 0-width pane). */}

                <ResizableHandle
                    className={twMerge('w-1 bg-transparent', !showResourcePanel && 'pointer-events-none opacity-0')}
                />

                {/* Collapsible right pane: collapsed (width 0) when closed, expanded to ~65% on open. The
                 * width transition is driven imperatively via the effect above (resize/collapse on
                 * `resourcePanelHandleRef`) so the chat narrows and this pane widens together. */}

                <ResizablePanel
                    collapsedSize="0%"
                    collapsible
                    defaultSize="0%"
                    elementRef={resourcePaneElementRef}
                    minSize="30%"
                    panelRef={resourcePanelHandleRef}
                >
                    <div className="size-full overflow-hidden">
                        {/* `resourcePanelMounted` (not `hasResourceContent` directly) — see the effect
                         * above for why the unmount lags the content going away by 320ms. */}

                        {resourcePanelMounted && <AiHubResourcePanel />}
                    </div>
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    );

    return (
        <LayoutContainer
            className="bg-surface-main"
            // No top `header` slot any more — both the chat title (which lived on the left of the
            // header) and the EnvironmentSelect (which lived on the right) have moved into their natural
            // homes: the title sits inside AiHubPanel's own header alongside the Ask/Build toggle,
            // and EnvironmentSelect lives in AiHubChatsSidebar's Chats row. This
            // lets both sidebars stretch top-to-bottom without a strip across the top of the layout.
            // Animation is off only while a peek is up: the preview must pop in instantly under the resting
            // pointer (an animated slide-in would leave a window where the pointer never "entered" the
            // aside, so its mouse-leave — which ends the peek — could never fire). Every other transition
            // (dock open, hide, and the peek's own slide-out) animates like any other page's sidebar.
            leftSidebarAnimate={!peekChatsSidebar}
            leftSidebarBody={<AiHubChatsSidebar />}
            // While peeking, the sidebar header carries a PIN control (in line with the "AI Hub" title) —
            // the peek overlay floats over the panel-header toggle that opened it, so that toggle can't be
            // clicked to dock the preview open. When docked open there is nothing here: the header toggle
            // is the close control, as on every other page.
            leftSidebarHeader={
                <Header
                    position="sidebar"
                    right={peekChatsSidebar ? <AiHubChatsSidebarPinButton /> : undefined}
                    title="AI Hub"
                />
            }
            // End the hover "peek" when the pointer leaves the floating sidebar — slides it back out.
            // Pinning (the header control) clears the peek separately via the store.
            leftSidebarOnMouseLeave={() => setChatsSidebarPeeking(false)}
            leftSidebarOpen={showChatsSidebar}
            // While hidden, a hover on the toggle floats the full sidebar OVER the content (no reflow) as a
            // preview. `leftSidebarOpen` stays false so the docked sidebar reserves no space.
            leftSidebarOverlay={peekChatsSidebar}
            leftSidebarWidth="64"
        >
            {/*
             * Runtime provider is hoisted up here (was previously instantiated inside HomePanel and Panel).
             * The home -> chat transition flips `mainBody` from `AiHubHomePanel` to the
             * `AiHubPanel` view; if each side owned its own provider, that flip would unmount the
             * provider mid-`onNew`, fire its cleanup useEffect, and abort the AG-UI agent run via
             * `cleanupForChatChange` — the user would land on the chat page with their
             * message but no streaming reply. Mounting the provider once at this level keeps the runtime
             * (and its turn AbortController) alive across the view swap.
             *
             * AiHubErrorBoundary stays here too, for the same reason — it catches the
             * `@assistant-ui/react` "unmount a fiber that is already unmounted" error in React 19, and
             * needs to wrap the runtime provider to fence those errors at a single point.
             */}

            <AiHubErrorBoundary open={true}>
                <AiHubRuntimeProvider>{mainBody}</AiHubRuntimeProvider>
            </AiHubErrorBoundary>
        </LayoutContainer>
    );
};

const AiHub = () => <AiHubContent />;

export default AiHub;
