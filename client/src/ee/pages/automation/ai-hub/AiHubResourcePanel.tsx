import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubAiAgentViewer from '@/ee/pages/automation/ai-hub/AiHubAiAgentViewer';
import AiHubWorkflowViewer from '@/ee/pages/automation/ai-hub/AiHubWorkflowViewer';
import WorkflowTabLabel from '@/ee/pages/automation/ai-hub/WorkflowTabLabel';
import {useAiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {BotIcon, PanelRightIcon, XIcon} from 'lucide-react';
import {useEffect, useRef} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

/**
 * The right-hand resource panel: a strip of workflow and AI Agent tabs over the active tab's view, framed as a floating
 * card. The strip scrolls horizontally, so the active tab is scrolled into view with the minimum movement on both
 * axes; otherwise a newly opened tab could land clipped past the strip's edge.
 */
const AiHubResourcePanel = () => {
    const activeTabRef = useRef<HTMLDivElement | null>(null);

    const {activeTabId, closeTab, openTabs, setActiveTab, setRightPanelOpen} = useAiHubTabsStore(
        useShallow((state) => ({
            activeTabId: state.activeTabId,
            closeTab: state.closeTab,
            openTabs: state.openTabs,
            setActiveTab: state.setActiveTab,
            setRightPanelOpen: state.setRightPanelOpen,
        }))
    );

    const activeTab = openTabs.find((tab) => tab.id === activeTabId);

    useEffect(() => {
        activeTabRef.current?.scrollIntoView({block: 'nearest', inline: 'nearest'});
    }, [activeTabId]);

    return (
        <div className="size-full bg-surface-main py-2 pr-2">
            <div className="flex size-full flex-col overflow-hidden rounded-xl border bg-surface-neutral-primary shadow-sm">
                <div className="flex min-h-12 items-center gap-1 border-b">
                    <div className="flex flex-1 items-stretch gap-0 overflow-x-auto">
                        {openTabs.map((tab) => {
                            const isActive = tab.id === activeTabId;

                            return (
                                <div
                                    className={twMerge(
                                        'flex items-center gap-1 border-b-2 border-transparent px-4 py-3 text-sm font-medium whitespace-nowrap text-content-neutral-secondary transition-colors',
                                        'hover:border-stroke-brand-primary hover:text-content-brand-primary',
                                        isActive && 'border-stroke-brand-primary text-content-brand-primary'
                                    )}
                                    key={tab.id}
                                    ref={isActive ? activeTabRef : undefined}
                                >
                                    {tab.kind === 'workflow' ? (
                                        <WorkflowTabLabel
                                            name={tab.name}
                                            onClick={() => setActiveTab(tab.id)}
                                            projectId={tab.projectId}
                                        />
                                    ) : (
                                        <button
                                            className="flex max-w-40 items-center gap-1"
                                            onClick={() => setActiveTab(tab.id)}
                                            type="button"
                                        >
                                            <BotIcon className="size-3.5 shrink-0" />

                                            <span className="truncate">{tab.name}</span>
                                        </button>
                                    )}

                                    <button
                                        aria-label={`Close ${tab.name}`}
                                        className="ml-1 rounded p-0.5 text-muted-foreground hover:bg-muted hover:text-foreground"
                                        onClick={() => closeTab(tab.id)}
                                        type="button"
                                    >
                                        <XIcon className="size-3" />
                                    </button>
                                </div>
                            );
                        })}
                    </div>

                    <div className="flex shrink-0 items-center gap-1 px-2">
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Close resource panel"
                                    icon={<PanelRightIcon />}
                                    onClick={() => setRightPanelOpen(false)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </TooltipTrigger>

                            <TooltipContent>Hide resources</TooltipContent>
                        </Tooltip>
                    </div>
                </div>

                {activeTab ? (
                    <div className="min-h-0 flex-1">
                        {activeTab.kind === 'workflow' ? (
                            <AiHubWorkflowViewer
                                projectId={activeTab.projectId}
                                projectWorkflowId={activeTab.projectWorkflowId}
                            />
                        ) : (
                            <AiHubAiAgentViewer
                                aiAgentId={activeTab.aiAgentId}
                                key={activeTab.aiAgentId}
                                name={activeTab.name}
                            />
                        )}
                    </div>
                ) : (
                    <div className="flex flex-1 items-center justify-center px-4 text-center text-sm text-muted-foreground">
                        Workflows and AI Agents you or the assistant open appear here.
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubResourcePanel;
