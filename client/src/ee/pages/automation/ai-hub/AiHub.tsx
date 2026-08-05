import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import AiHubErrorBoundary from '@/ee/pages/automation/ai-hub/AiHubErrorBoundary';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import AiHubTasksSidebarCollapseButton from '@/ee/pages/automation/ai-hub/AiHubTasksSidebarCollapseButton';
import AiHubTasksSidebarRail from '@/ee/pages/automation/ai-hub/AiHubTasksSidebarRail';
import {useResetAiHubStoresOnWorkspaceChange} from '@/ee/pages/automation/ai-hub/hooks/useResetAiHubStoresOnWorkspaceChange';
import {AiHubRuntimeProvider} from '@/ee/pages/automation/ai-hub/runtime-providers/AiHubRuntimeProvider';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import AiHubTasksSidebar from '@/ee/pages/automation/ai-hub/tasks/AiHubTasksSidebar';
import useRecordReferencedArtifacts from '@/ee/pages/automation/ai-hub/tasks/hooks/useRecordReferencedArtifacts';
import {useSwitchTask} from '@/ee/pages/automation/ai-hub/tasks/hooks/useSwitchTask';
import {useAiHubTasksQuery} from '@/ee/pages/automation/ai-hub/tasks/hooks/useTasks';
import {aiHubTasksStore, useAiHubTasksStore} from '@/ee/pages/automation/ai-hub/tasks/stores/useAiHubTasksStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {aiChatAskedQuestionsStore} from '@/shared/components/ai-chat/stores/useAiChatAskedQuestionsStore';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useRef} from 'react';
import {type PanelImperativeHandle} from 'react-resizable-panels';
import {useNavigate, useParams} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const AiHubContent = () => {
    const setMode = useAiHubStore((state) => state.setMode);
    const generateTaskId = useAiHubStore((state) => state.generateTaskId);
    const resetMessages = useAiHubStore((state) => state.resetMessages);

    const rightPanelOpen = useAiHubTabsStore((state) => state.rightPanelOpen);
    const tasksSidebarCollapsed = useAiHubTabsStore((state) => state.tasksSidebarCollapsed);
    const tasksSidebarPeeking = useAiHubTabsStore((state) => state.tasksSidebarPeeking);
    const setActiveTabsTaskId = useAiHubTabsStore((state) => state.setActiveTaskId);
    const setTasksSidebarPeeking = useAiHubTabsStore((state) => state.setTasksSidebarPeeking);

    const currentTaskId = useAiHubTasksStore((state) => state.currentTaskId);
    // Flip to the thread view as soon as there are messages, not only once the DB task id arrives. On the
    // home→task transition, AiHubRuntimeProvider.onNew adds the user message before awaiting the (possibly
    // slow) task creation — gating on messages here makes that switch feel instant instead of leaving the
    // user on a frozen home page. The selector returns a boolean so it only re-renders on the 0↔non-empty
    // edge, not on every streamed chunk.
    const hasMessages = useAiHubStore((state) => state.messages.length > 0);
    const hasActiveTask = currentTaskId != null || hasMessages;

    // The resource (right) panel only shows in the split view — an active task plus an open right panel.
    const showResourcePanel = hasActiveTask && rightPanelOpen;

    // Opening the resource panel collapses the left Tasks sidebar to a thin rail (`tasksSidebarCollapsed`)
    // so the chat + resource panels reclaim the width. No delay on either edge: the rail/sidebar swap
    // tracks `showResourcePanel` directly so the sidebar's padding ease runs on the SAME 300ms clock as
    // the chat's slide — otherwise the panel header (pinned to the sidebar-affected left edge) and the
    // centered body move on different clocks and look out of sync. Open stays instant via
    // `leftSidebarAnimate={!showSidebarRail}` (false while the rail shows); close animates the reappear.
    const showSidebarRail = showResourcePanel && tasksSidebarCollapsed;

    // Coordinated open/close for the RIGHT resource panel (the left sidebar / rail behavior is untouched).
    // The resource pane is a COLLAPSIBLE panel that's always mounted (when a task is active) so its width
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

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const {data: tasks, isLoading: tasksLoading} = useAiHubTasksQuery(
        currentWorkspaceId,
        currentEnvironmentId,
        'ACTIVE'
    );

    const navigate = useNavigate();
    const {taskId: urlTaskIdParam} = useParams<{taskId?: string}>();
    const switchTask = useSwitchTask();

    // Deep link to /tasks/:id lands here with an empty store: the URL→store sync effect below can only
    // switch to the task once the (async) tasks query resolves. Without this flag the home panel would
    // flash in that gap — hold the loader instead, but only while the id can still resolve (query in
    // flight, or the task exists in the resolved list). An unknown/foreign id falls back to the home
    // view, matching the sync effect's silent no-op for missing tasks.
    const pendingUrlTask =
        !hasActiveTask &&
        !!urlTaskIdParam &&
        (tasksLoading || (tasks?.some((task) => String(task.id) === urlTaskIdParam) ?? false));

    // Active task lookup happens inside AiHubPanel now — title + workflow-chat badge
    // both live there alongside the Ask/Build toggle. The CC root no longer needs the title because the
    // top header strip is gone.

    // Hoisted to this level so the workspace-store reset effect runs once for the whole AI Hub
    // surface — children (HomePanel, Panel, ResourcePanel) used to register it independently, which fired
    // the reset multiple times on workspace change.
    useResetAiHubStoresOnWorkspaceChange();

    // Records each open right-panel tab as a `ai_hub_task_artifact` so it shows up in the sidebar
    // artifact list. Bridges the gap between the UI-only tab state and the persistent artifact log —
    // covers BOTH user-driven attachment (composer plus-button → opens tab) and agent-driven attachment
    // (chat tool calls that open files/workflows). The hook is a no-op when there's no active
    // task (home view); it resumes recording once a task is created.
    //
    // Disabled for WORKFLOW_CHAT tasks: they auto-open their bound workflow in the right panel, but a
    // workflow chat routes through a webhook (not the LLM) and must not accrue artifacts — otherwise that
    // auto-opened workflow would be recorded as a (spurious) artifact of the chat.
    const currentTaskIsWorkflowChat = tasks?.find((task) => task.id === currentTaskId)?.kind === 'WORKFLOW_CHAT';

    useRecordReferencedArtifacts(currentTaskId, currentWorkspaceId ?? 0, !currentTaskIsWorkflowChat);

    useEffect(() => {
        setMode(MODE.BUILD);
    }, [setMode]);

    /*
     * Mirror the active task into the tabs store so per-task tab snapshots can be
     * saved/restored across switches. Without this wiring the resource-panel tabs would persist
     * VISUALLY across task changes (the store is global) but they wouldn't be associated with
     * any task — switching from /tasks/A back to /tasks/B would still show
     * task A's tabs because the store doesn't know which task it's mirroring.
     */
    useEffect(() => {
        setActiveTabsTaskId(currentTaskId);
    }, [currentTaskId, setActiveTabsTaskId]);

    /*
     * Reset the askUserQuestion answered-state store on task switch. The store is keyed by question
     * content fingerprint (question text + option labels), which is content-stable per question instance
     * but NOT scoped to a task. Two different tasks producing the same question shape (e.g. "Which Slack
     * channel?" with the same channel list) would otherwise share the answered state — task B would see
     * task A's answer as already-submitted, hide the buttons, and confuse the user. The workspace-change
     * reset hook already calls reset() for cross-workspace switches; this effect covers in-workspace
     * task switches.
     */
    useEffect(() => {
        aiChatAskedQuestionsStore.getState().reset();
    }, [currentTaskId]);

    // Note: workflow-chat tasks intentionally do NOT auto-open their bound workflow in the right resource
    // panel. A workflow chat is a chat — popping the full workflow editor (Publish/Deploy, editable nodes)
    // on every open was heavy and surprising. The workflow can still be opened manually via the right
    // panel's resource picker if the user wants to inspect it.

    /*
     * URL <-> store sync — single effect with explicit priority ordering.
     *
     * Three flow shapes need to coexist without cross-firing:
     *
     *   (a) STORE-DRIVEN — `storeChanged` is true. Caused by a sidebar task row click
     *       (`switchTask` writes the store), the auto-create path inside `onNew`
     *       (`createAiHubTask` then `setCurrentTaskId`), or an explicit reset (delete /
     *       archive of the active task). The URL must catch up. ALWAYS HANDLED FIRST so the
     *       URL→store invariant below cannot revert the store back to whatever the URL still says.
     *
     *   (b) URL-DRIVEN STANDING — URL has a task id and the store doesn't yet match. Triggered
     *       on cold-mount deep links and on every render until the tasks query resolves. This
     *       has to be a STANDING condition (not gated on cross-render diff) because the tasks
     *       list is async and may arrive after the first render — gating on `urlChanged` made the
     *       deep-link case silently no-op whenever the data landed on render 2+.
     *
     *   (c) URL → home — URL has no task id but the store still does. Reset the store.
     *       Fires under TWO conditions:
     *         1. URL just transitioned to home (`urlChanged`) — sidebar AI Hub icon click.
     *         2. Initial effect run on mount (`isFirstEffectRef`) — catches the remount case where
     *            the user clicks a task, navigates to another page, then returns to
     *            /ai-hub with `urlTaskIdParam = undefined` and a stale
     *            `currentTaskId` left in the global store from before navigation.
     *       NOT a fully-standing condition: between branch (a) calling `navigate()` and React Router's
     *       URL context catching up, there's a render where store has the new id but `useParams`
     *       still returns `undefined`. A standing branch (c) would erroneously reset the store there,
     *       triggering an A → C → A bounce loop (URL flips home → task → home → task
     *       on every send-from-home flow).
     *
     * The previous shape ran (b) before (a). When the user clicked a sibling task row from
     * /tasks/17 to /tasks/18, the store changed to 18 but the URL was still "17"; (b)
     * saw the mismatch, found task 17 in the list, and called `switchTask(17)`,
     * which clobbered the user's intent. Symptom: clicking a different task appeared to do
     * nothing (the panel stayed on 17). The order swap below fixes that without breaking deep links.
     */
    const previousUrlParamRef = useRef(urlTaskIdParam);
    const previousStoreIdRef = useRef(currentTaskId);
    const isFirstEffectRef = useRef(true);

    useEffect(() => {
        const urlChanged = urlTaskIdParam !== previousUrlParamRef.current;
        const storeChanged = currentTaskId !== previousStoreIdRef.current;
        const isFirstEffect = isFirstEffectRef.current;

        previousUrlParamRef.current = urlTaskIdParam;
        previousStoreIdRef.current = currentTaskId;
        isFirstEffectRef.current = false;

        // (a) STORE-DRIVEN: highest priority. Push URL to match the store.
        if (storeChanged) {
            if (currentTaskId != null && String(currentTaskId) !== urlTaskIdParam) {
                navigate(`/automation/ai-hub/tasks/${currentTaskId}`);
            } else if (currentTaskId == null && urlTaskIdParam) {
                navigate('/automation/ai-hub');
            }

            return;
        }

        // (b) URL-DRIVEN STANDING: URL has an id, keep store aligned. Self-heals across renders.
        if (urlTaskIdParam) {
            const idNum = Number(urlTaskIdParam);

            if (!Number.isNaN(idNum) && idNum !== currentTaskId) {
                const task = tasks?.find((candidate) => candidate.id === idNum);

                if (task) {
                    void switchTask(task);
                }
                // No task found: fall through silently. Next tasks refetch retries.
            }

            return;
        }

        // (c) URL → home. Reset store on (1) URL transition to home or (2) first effect run after mount
        // with a stale store value. NOT on intermediate renders during branch (a)'s navigate() flush.
        if ((urlChanged || isFirstEffect) && currentTaskId != null) {
            aiHubTasksStore.getState().setCurrentTaskId(undefined);
            resetMessages();
            generateTaskId();
        }
    }, [tasks, currentTaskId, generateTaskId, navigate, resetMessages, switchTask, urlTaskIdParam]);

    // Home view: no active task yet — show only the centered composer. The first message sent here
    // auto-creates a task (see AiHubRuntimeProvider.onNew), which flips this branch to the
    // panel view containing the thread. Personal Agents and Workflow Chats are full-page routes — they live
    // outside this component (see /automation/ai-hub/personal-agents and /workflow-chats).
    // While a deep-linked task is still resolving, render the same four-dot pulse as
    // LazyLoadWrapper's suspense fallback so the route-chunk load and the task resolution read as one
    // continuous loading state instead of loader → home flash → task.
    const mainBody = pendingUrlTask ? (
        <div className="flex size-full items-center justify-center p-8">
            <div className="flex animate-pulse space-x-2">
                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>

                <div className="size-4 rounded-full bg-content-neutral-tertiary"></div>
            </div>
        </div>
    ) : !hasActiveTask ? (
        <AiHubHomePanel />
    ) : (
        <div className="flex size-full">
            {/* Collapsed Tasks sidebar (rail), shown while the resource panel is open and the sidebar is
             * collapsed. It appears/disappears instantly — the sidebar is intentionally not animated; only
             * the chat/resource split animates. Expanding it (its "show tasks" button) flips
             * `tasksSidebarCollapsed`, which brings the full LayoutContainer sidebar back. */}

            {/* Hovering the rail "peeks" the full Tasks sidebar: it floats in as an overlay (see
             * `leftSidebarOverlay` below) without reflowing the chat, and collapses back when the pointer
             * leaves it. The pointer-leave that closes the peek is on the overlay aside itself. */}

            {showSidebarRail && <AiHubTasksSidebarRail onMouseEnter={() => setTasksSidebarPeeking(true)} />}

            <ResizablePanelGroup className="min-w-0 flex-1" orientation="horizontal">
                <ResizablePanel elementRef={chatPaneElementRef} minSize="25%">
                    <AiHubPanel />
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
                        <AiHubResourcePanel />
                    </div>
                </ResizablePanel>
            </ResizablePanelGroup>
        </div>
    );

    return (
        <LayoutContainer
            className="bg-surface-main"
            // No top `header` slot any more — both the task title (which lived on the left of the
            // header) and the EnvironmentSelect (which lived on the right) have moved into their natural
            // homes: the title sits inside AiHubPanel's own header alongside the Ask/Build toggle,
            // and EnvironmentSelect lives in AiHubTasksSidebar's Tasks row. This
            // lets both sidebars stretch top-to-bottom without a strip across the top of the layout.
            // Animate the sidebar only when the FULL sidebar is the one being shown (i.e. on the close /
            // rail→sidebar transition). That eases the content padding from the rail's 48px back to the
            // sidebar's 256px, so the chat slides to its final width instead of bumping 208px in one frame.
            // On open (sidebar→rail) animation stays off, so the collapse is instant and doesn't flash.
            leftSidebarAnimate={!showSidebarRail}
            leftSidebarBody={<AiHubTasksSidebar />}
            // The collapse-to-rail control lives in the header's `right` slot so it sits in line with the
            // "AI Hub" title. Shown only while the resource panel is open — the only state where collapsing
            // to the rail is meaningful.
            leftSidebarHeader={
                <Header
                    position="sidebar"
                    right={showResourcePanel ? <AiHubTasksSidebarCollapseButton /> : undefined}
                    title="AI Hub"
                />
            }
            // End the hover "peek" when the pointer leaves the floating sidebar — collapses it back to the
            // rail. Pinning (the header control) clears the peek separately via the store.
            leftSidebarOnMouseLeave={() => setTasksSidebarPeeking(false)}
            // Hide the LayoutContainer left sidebar while the collapsed rail stands in for it (resource
            // panel open + collapsed). Otherwise the full sidebar renders here as usual.
            leftSidebarOpen={!showSidebarRail}
            // While collapsed to the rail, a hover floats the full sidebar OVER the content (no reflow) as
            // a preview. `leftSidebarOpen` stays false so the docked sidebar reserves no space.
            leftSidebarOverlay={showSidebarRail && tasksSidebarPeeking}
            leftSidebarWidth="64"
        >
            {/*
             * Runtime provider is hoisted up here (was previously instantiated inside HomePanel and Panel).
             * The home -> task transition flips `mainBody` from `AiHubHomePanel` to the
             * `AiHubPanel` view; if each side owned its own provider, that flip would unmount the
             * provider mid-`onNew`, fire its cleanup useEffect, and abort the AG-UI agent run via
             * `cleanupForTaskChange` — the user would land on the task page with their
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
