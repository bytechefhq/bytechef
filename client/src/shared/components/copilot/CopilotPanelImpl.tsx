import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import CopilotPanelBoundary from '@/shared/components/copilot/CopilotPanelBoundary';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {useAiDefaultModelQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {type SuggestionConfig} from '@assistant-ui/react';
import {BotMessageSquareIcon, BrainCircuitIcon, MessageSquareXIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const ANIMATION_DURATION_MS = 300;

const AI_PROVIDERS_SETTINGS_PATH = '/automation/settings/ai-providers';

const COPILOT_SUGGESTIONS: SuggestionConfig[] = [
    {
        label: 'does end-to-end.',
        prompt: 'Describe what this workflow does end-to-end.',
        title: 'Describe what this workflow',
    },
    {
        label: 'of this action are required?',
        prompt: 'Which properties of this action are required?',
        title: 'Which properties',
    },
    {
        label: 'that can send an email',
        prompt: 'Search for an action that can send an email',
        title: 'Search for an action',
    },
    {
        label: 'conditional branching in workflows?',
        prompt: 'How do I implement conditional branching in workflows?',
        title: 'How do I implement',
    },
];

interface CopilotPanelProps {
    className?: string;
    headerClassName?: string;
    onClose?: () => void;
    open: boolean;
    source?: Source;
}

export const CopilotPanelContent = ({className, headerClassName, onClose, source}: Omit<CopilotPanelProps, 'open'>) => {
    const {
        composerPlaceholder,
        context,
        generateConversationId,
        resetMessages,
        selectedLlmModel,
        selectedLlmProvider,
        setComposerPlaceholder,
        setContext,
        setSelectedLlm,
    } = useCopilotStore(
        useShallow((state) => ({
            composerPlaceholder: state.composerPlaceholder,
            context: state.context,
            generateConversationId: state.generateConversationId,
            resetMessages: state.resetMessages,
            selectedLlmModel: state.selectedLlmModel,
            selectedLlmProvider: state.selectedLlmProvider,
            setComposerPlaceholder: state.setComposerPlaceholder,
            setContext: state.setContext,
            setSelectedLlm: state.setSelectedLlm,
        }))
    );
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const location = useLocation();
    const navigate = useNavigate();

    const {data: defaultModelData, isPending: isDefaultModelPending} = useAiDefaultModelQuery({
        environment: String(currentEnvironmentId),
    });

    const hasEnabledProvider = defaultModelData?.aiDefaultModel != null;

    const handleCleanMessages = () => {
        resetMessages();
        generateConversationId();
    };

    // Shared by the close button and the navigation effect below. Local-panel surfaces pass an onClose and
    // drive their own open flag, so this only ever applies to the global panel mounted in App.tsx.
    const closeGlobalPanel = useCallback(() => {
        const {globalPanelConversationToken, restoreConversationState, setGlobalPanelConversationToken} =
            useCopilotStore.getState();

        // Only useOpenCopilot.ts pushes for the global panel, and only when the panel was actually closed at
        // the time — six other call sites (DataTable.tsx, KnowledgeBase.tsx, ContextStoreSources.tsx,
        // AiSkillDetail.tsx, the EE CustomComponentDetail.tsx, and the workflow-editor toolbar toggle) open
        // this same panel by setting context and flipping setCopilotPanelOpen directly, without pushing
        // anything. globalPanelConversationToken is null in exactly that case, so falling through to the
        // hardcoded reset below (rather than unconditionally restoring whenever the stack happens to be
        // non-empty) is what stops this close from popping an entry some other surface pushed.
        //
        // The token is cleared right after use (both branches) — otherwise it would stay set to this open's
        // (now popped, or never valid) token forever, since nothing else in production code clears it. A
        // later close of a direct-open session would then find a stale truthy token, take the restore branch
        // instead of the fallback, no-op against the (by-then-mismatched) stack top, and skip resetting the
        // context — leaking this surface's context into whatever opens the global panel next.
        //
        // Note the clear only happens here. Three surfaces close the global panel without going through this
        // function (useProject, AiSkillDetail, the EE useIntegration), but all three are unmount cleanups that
        // coincide with a pathname change, and the navigation effect below routes that back through here. A
        // future surface that closes the panel directly WITHOUT navigating would reopen the stale-token window
        // described above, so it must clear the token itself.
        if (globalPanelConversationToken) {
            // Something was open underneath — hand it back rather than discarding it.
            restoreConversationState(globalPanelConversationToken);
        } else {
            setContext({
                mode: MODE.ASK,
                parameters: {},
                source: Source.WORKFLOW_EDITOR,
            });
            setComposerPlaceholder(undefined);
        }

        setGlobalPanelConversationToken(null);
        setCopilotPanelOpen(false);
    }, [setComposerPlaceholder, setContext, setCopilotPanelOpen]);

    const handleCloseClick = () => {
        if (onClose) {
            onClose();
        } else {
            closeGlobalPanel();
        }
    };

    const previousPathnameRef = useRef(location.pathname);

    useEffect(() => {
        if (previousPathnameRef.current === location.pathname) {
            return;
        }

        previousPathnameRef.current = location.pathname;

        generateConversationId();
        resetMessages();

        /*
         * Navigating away closes the global panel. Without this it stays open on the new page while
         * context.source still names the surface the user left — and CopilotRuntimeProvider derives both the
         * agent id and the /ai/chat/{source} URL from that value, so the first message sent on the new page
         * would be routed to the previous page's agent carrying the previous page's parameters. Closing
         * fixes the stale panel and the mis-routing together.
         *
         * Local-panel surfaces are left alone: they pass an onClose, own their own open flag, and live inside
         * dialogs that unmount on navigation anyway.
         */
        if (!onClose) {
            closeGlobalPanel();
        }
    }, [closeGlobalPanel, generateConversationId, location.pathname, onClose, resetMessages]);

    return (
        <div className={twMerge('relative h-full min-h-[50vh] w-[450px] bg-surface-main', className)}>
            <div className={twMerge('flex items-center justify-between px-4 py-3', headerClassName)}>
                <div className="flex items-center space-x-1">
                    <BotMessageSquareIcon className="size-6" /> <h4>AI Copilot</h4>
                </div>

                <div className="flex items-center gap-1">
                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                icon={<MessageSquareXIcon />}
                                onClick={handleCleanMessages}
                                size="icon"
                                variant="ghost"
                            />
                        </TooltipTrigger>

                        <TooltipContent>Clean messages</TooltipContent>
                    </Tooltip>

                    <Button
                        aria-label="Close Copilot panel"
                        icon={<XIcon />}
                        onClick={handleCloseClick}
                        size="icon"
                        variant="ghost"
                    />
                </div>
            </div>

            <div className="absolute inset-x-0 top-16 -bottom-3 -mx-1">
                {!isDefaultModelPending && !hasEnabledProvider ? (
                    <div className="flex h-full flex-col items-center justify-center gap-4 px-8 text-center">
                        <BrainCircuitIcon className="size-10 text-content-neutral-secondary" />

                        <div className="flex flex-col gap-1">
                            <p className="text-sm font-medium text-content-neutral-primary">No AI providers enabled</p>

                            <p className="text-sm text-content-neutral-secondary">
                                Enable an AI provider to start using the AI Copilot.
                            </p>
                        </div>

                        <Button onClick={() => navigate(AI_PROVIDERS_SETTINGS_PATH)}>Go to AI Providers</Button>
                    </div>
                ) : (
                    <CopilotRuntimeProvider source={source} suggestions={COPILOT_SUGGESTIONS}>
                        <Thread
                            composerActions={
                                !source ? (
                                    <ModeSwitch
                                        build={context?.mode === MODE.BUILD}
                                        onBuildChange={(build) =>
                                            setContext({...context, mode: build ? MODE.BUILD : MODE.ASK})
                                        }
                                    />
                                ) : null
                            }
                            composerPlaceholder={composerPlaceholder}
                            dataComponents={aiChatDataComponents}
                            leadingComposerActions={
                                currentWorkspaceId != null ? (
                                    <ModelPicker
                                        defaultModel={defaultModelData?.aiDefaultModel?.model ?? null}
                                        defaultProvider={defaultModelData?.aiDefaultModel?.provider ?? null}
                                        environment={currentEnvironmentId}
                                        onChange={(provider, model) => {
                                            writeLastUsedModel(currentWorkspaceId, provider, model);
                                            setSelectedLlm(provider, model);
                                        }}
                                        selectedModel={
                                            selectedLlmModel ?? readLastUsedModel(currentWorkspaceId)?.model ?? null
                                        }
                                        selectedProvider={
                                            selectedLlmProvider ??
                                            readLastUsedModel(currentWorkspaceId)?.provider ??
                                            null
                                        }
                                    />
                                ) : null
                            }
                        />
                    </CopilotRuntimeProvider>
                )}
            </div>
        </div>
    );
};

const CopilotPanel = ({className, headerClassName, onClose, open, source}: CopilotPanelProps) => {
    const [shouldRender, setShouldRender] = useState(open);
    const [isVisible, setIsVisible] = useState(open);

    const isSlideAnimation = className?.split(/\s+/).some((cls) => cls === 'fixed' || cls === 'absolute');

    const contentClassName = isSlideAnimation
        ? twMerge(
              'transition-transform duration-300 ease-in-out',
              isVisible ? 'translate-x-0' : 'translate-x-full',
              className
          )
        : className;

    useEffect(() => {
        let outerRafId: number | undefined;
        let innerRafId: number | undefined;
        let timerId: ReturnType<typeof setTimeout> | undefined;

        if (open) {
            setShouldRender(true);

            outerRafId = requestAnimationFrame(() => {
                innerRafId = requestAnimationFrame(() => {
                    setIsVisible(true);
                });
            });
        } else {
            setIsVisible(false);

            timerId = setTimeout(() => setShouldRender(false), ANIMATION_DURATION_MS);
        }

        return () => {
            if (outerRafId !== undefined) {
                cancelAnimationFrame(outerRafId);
            }

            if (innerRafId !== undefined) {
                cancelAnimationFrame(innerRafId);
            }

            if (timerId !== undefined) {
                clearTimeout(timerId);
            }
        };
    }, [open]);

    return (
        <CopilotPanelBoundary open={open}>
            <div
                className={twMerge(
                    'overflow-hidden',
                    !isSlideAnimation && 'h-full transition-[width] duration-300 ease-in-out',
                    !isSlideAnimation && (isVisible ? 'w-[450px]' : 'w-0')
                )}
            >
                {shouldRender && (
                    <CopilotPanelContent
                        className={contentClassName}
                        headerClassName={headerClassName}
                        onClose={onClose}
                        source={source}
                    />
                )}
            </div>
        </CopilotPanelBoundary>
    );
};

export default CopilotPanel;
