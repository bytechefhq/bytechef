import Button from '@/components/Button/Button';
import {Thread} from '@/components/assistant-ui/thread';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import ModeSwitch from '@/shared/components/ModeSwitch/ModeSwitch';
import {aiChatDataComponents} from '@/shared/components/ai-chat/messages/aiChatDataComponents';
import ModelPicker from '@/shared/components/ai/model-picker/ModelPicker';
import {writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {findAvailableRememberedModel} from '@/shared/components/ai/model-picker/resolveRememberedModel';
import CopilotPanelBoundary from '@/shared/components/copilot/CopilotPanelBoundary';
import {CopilotRuntimeProvider} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    MODE,
    Source,
    isWorkflowEditorSource,
    useCopilotStore,
} from '@/shared/components/copilot/stores/useCopilotStore';
import getCopilotSuggestions from '@/shared/components/copilot/utils/getCopilotSuggestions';
import {AI_PROVIDERS_LINK_LABEL, AI_PROVIDERS_SETTINGS_PATH} from '@/shared/constants';
import {useAiDefaultModelQuery, useAiProviderCatalogQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BotMessageSquareIcon, BrainCircuitIcon, MessageSquareXIcon, XIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const ANIMATION_DURATION_MS = 300;

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
    const compactHeader = useCopilotPanelStore((state) => state.compactHeader);
    const setCopilotPanelOpen = useCopilotPanelStore((state) => state.setCopilotPanelOpen);
    const location = useLocation();
    const navigate = useNavigate();

    const {data: defaultModelData, isPending: isDefaultModelPending} = useAiDefaultModelQuery({
        environment: String(currentEnvironmentId),
    });

    const hasEnabledProvider = defaultModelData?.aiDefaultModel != null;

    const catalogEnvironment = context?.environmentId ?? currentEnvironmentId;

    const {data: providerCatalogData} = useAiProviderCatalogQuery(
        {environment: String(catalogEnvironment)},
        {enabled: catalogEnvironment >= 0}
    );

    const displayedModel = useMemo(() => {
        if (selectedLlmProvider != null && selectedLlmModel != null) {
            return {model: selectedLlmModel, provider: selectedLlmProvider};
        }

        return findAvailableRememberedModel(currentWorkspaceId, providerCatalogData?.aiProviderCatalog);
    }, [currentWorkspaceId, providerCatalogData, selectedLlmModel, selectedLlmProvider]);

    const copilotSuggestions = useMemo(
        () => getCopilotSuggestions(source || context?.source),
        [context?.source, source]
    );

    const handleCleanMessages = () => {
        resetMessages();
        generateConversationId();
    };

    const closeGlobalPanel = useCallback(() => {
        const {
            context: currentContext,
            globalPanelConversationToken,
            restoreConversationState,
            setGlobalPanelConversationToken,
        } = useCopilotStore.getState();

        if (globalPanelConversationToken) {
            restoreConversationState(globalPanelConversationToken);
        } else {
            setContext({
                mode: isWorkflowEditorSource(currentContext.source) ? currentContext.mode : MODE.ASK,
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

        if (!onClose) {
            closeGlobalPanel();
        }
    }, [closeGlobalPanel, generateConversationId, location.pathname, onClose, resetMessages]);

    return (
        <div className={twMerge('relative h-full min-h-[50vh] w-[450px] bg-surface-main', className)}>
            <div
                className={twMerge(
                    'flex items-center justify-between',
                    compactHeader ? 'px-4 py-2.5' : 'p-4',
                    headerClassName
                )}
            >
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

                        <Button onClick={() => navigate(AI_PROVIDERS_SETTINGS_PATH)}>{AI_PROVIDERS_LINK_LABEL}</Button>
                    </div>
                ) : (
                    <CopilotRuntimeProvider source={source} suggestions={copilotSuggestions}>
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
                                        selectedModel={displayedModel?.model ?? null}
                                        selectedProvider={displayedModel?.provider ?? null}
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
