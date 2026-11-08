import Button from '@/components/Button/Button';
import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import AiHubAiAgentViewer from '@/ee/pages/automation/ai-hub/AiHubAiAgentViewer';
import AiHubDataTableViewer from '@/ee/pages/automation/ai-hub/AiHubDataTableViewer';
import AiHubFilePicker from '@/ee/pages/automation/ai-hub/AiHubFilePicker';
import AiHubFileViewer from '@/ee/pages/automation/ai-hub/AiHubFileViewer';
import AiHubKnowledgeBaseViewer from '@/ee/pages/automation/ai-hub/AiHubKnowledgeBaseViewer';
import AiHubWorkflowExecutionViewer from '@/ee/pages/automation/ai-hub/AiHubWorkflowExecutionViewer';
import AiHubWorkflowViewer from '@/ee/pages/automation/ai-hub/AiHubWorkflowViewer';
import WorkflowTabLabel from '@/ee/pages/automation/ai-hub/WorkflowTabLabel';
import {
    AiHubTabType,
    AiHubViewModeType,
    useAiHubTabsStore,
} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import CustomComponentDetail from '@/ee/pages/settings/platform/custom-components/CustomComponentDetail';
import ProjectCodeWorkflowDetail from '@/pages/platform/code-workflow/ProjectCodeWorkflowDetail';
import {
    BlocksIcon,
    BotIcon,
    CodeIcon,
    DatabaseIcon,
    DownloadIcon,
    ExternalLinkIcon,
    FileTextIcon,
    HistoryIcon,
    LayersIcon,
    PanelRightIcon,
    XIcon,
} from 'lucide-react';
import {ReactNode, useEffect, useRef} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

const TAB_ICONS: Record<Exclude<AiHubTabType['kind'], 'workflow'>, ReactNode> = {
    aiAgent: <BotIcon className="size-3.5 shrink-0" />,
    codeWorkflow: <CodeIcon className="size-3.5 shrink-0" />,
    customComponent: <BlocksIcon className="size-3.5 shrink-0" />,
    dataTable: <DatabaseIcon className="size-3.5 shrink-0" />,
    file: <FileTextIcon className="size-3.5 shrink-0" />,
    knowledgeBase: <LayersIcon className="size-3.5 shrink-0" />,
    workflowExecution: <HistoryIcon className="size-3.5 shrink-0" />,
};

const TEXTUAL_FILE_EXTENSIONS = new Set([
    'css',
    'csv',
    'htm',
    'html',
    'java',
    'js',
    'json',
    'jsx',
    'markdown',
    'md',
    'py',
    'sql',
    'ts',
    'tsx',
    'txt',
    'xml',
    'yaml',
    'yml',
]);

const isTextualFileName = (name: string): boolean => {
    const dotIndex = name.lastIndexOf('.');

    if (dotIndex < 0) {
        return false;
    }

    return TEXTUAL_FILE_EXTENSIONS.has(name.slice(dotIndex + 1).toLowerCase());
};

const AiHubResourcePanelTabContent = ({tab}: {tab: AiHubTabType}) => {
    switch (tab.kind) {
        case 'aiAgent':
            return <AiHubAiAgentViewer aiAgentId={tab.aiAgentId} key={tab.aiAgentId} name={tab.name} />;
        case 'codeWorkflow':
            return (
                <ProjectCodeWorkflowDetail
                    headerless
                    key={tab.projectId}
                    language={tab.language}
                    projectId={tab.projectId}
                />
            );
        case 'customComponent':
            return <CustomComponentDetail customComponentId={tab.customComponentId} key={tab.customComponentId} />;
        case 'dataTable':
            return <AiHubDataTableViewer dataTableId={tab.dataTableId} key={tab.dataTableId} name={tab.name} />;
        case 'file':
            return <AiHubFileViewer fileId={tab.fileId} key={tab.fileId} name={tab.name} viewMode={tab.viewMode} />;
        case 'knowledgeBase':
            return (
                <AiHubKnowledgeBaseViewer
                    key={tab.knowledgeBaseId}
                    knowledgeBaseId={tab.knowledgeBaseId}
                    name={tab.name}
                />
            );
        case 'workflowExecution':
            return (
                <AiHubWorkflowExecutionViewer
                    key={tab.workflowExecutionId}
                    workflowExecutionId={tab.workflowExecutionId}
                />
            );
        case 'workflow':
            return <AiHubWorkflowViewer projectId={tab.projectId} projectWorkflowId={tab.projectWorkflowId} />;
    }
};

const AiHubResourcePanel = () => {
    const activeTabRef = useRef<HTMLDivElement | null>(null);

    const {activeTabId, closeTab, openTabs, setActiveTab, setRightPanelOpen, setViewMode} = useAiHubTabsStore(
        useShallow((state) => ({
            activeTabId: state.activeTabId,
            closeTab: state.closeTab,
            openTabs: state.openTabs,
            setActiveTab: state.setActiveTab,
            setRightPanelOpen: state.setRightPanelOpen,
            setViewMode: state.setViewMode,
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
                                            {TAB_ICONS[tab.kind]}

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
                        <AiHubFilePicker />

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
                    <>
                        {activeTab.kind === 'file' && (
                            <div className="flex items-center justify-end gap-2 border-b px-2 py-1">
                                <ToggleGroup
                                    onValueChange={(value) => {
                                        if (value) {
                                            setViewMode(activeTab.id, value as AiHubViewModeType);
                                        }
                                    }}
                                    size="sm"
                                    type="single"
                                    value={activeTab.viewMode}
                                >
                                    {isTextualFileName(activeTab.name) && (
                                        <ToggleGroupItem value="editor">Editor</ToggleGroupItem>
                                    )}

                                    <ToggleGroupItem value="preview">Preview</ToggleGroupItem>

                                    {isTextualFileName(activeTab.name) && (
                                        <ToggleGroupItem value="split">Split</ToggleGroupItem>
                                    )}
                                </ToggleGroup>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Link
                                            aria-label="Open in Asset Files"
                                            className="inline-flex size-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                                            to={`/automation/asset-files/${activeTab.fileId}`}
                                        >
                                            <ExternalLinkIcon className="size-4" />
                                        </Link>
                                    </TooltipTrigger>

                                    <TooltipContent>Open in Asset Files</TooltipContent>
                                </Tooltip>

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <a
                                            aria-label="Download file"
                                            className="inline-flex size-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                                            download={activeTab.name}
                                            href={`/api/automation/internal/asset-files/${activeTab.fileId}/content`}
                                        >
                                            <DownloadIcon className="size-4" />
                                        </a>
                                    </TooltipTrigger>

                                    <TooltipContent>Download</TooltipContent>
                                </Tooltip>
                            </div>
                        )}

                        <div className="min-h-0 flex-1">
                            <AiHubResourcePanelTabContent tab={activeTab} />
                        </div>
                    </>
                ) : (
                    <div className="flex flex-1 items-center justify-center px-4 text-center text-sm text-muted-foreground">
                        Workflows, AI Agents and files you or the assistant open appear here.
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubResourcePanel;
