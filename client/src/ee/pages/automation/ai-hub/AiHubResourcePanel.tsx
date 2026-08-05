import Button from '@/components/Button/Button';
import {ToggleGroup, ToggleGroupItem} from '@/components/ui/toggle-group';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
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
import AiSkillDetail from '@/pages/automation/ai/skills/components/AiSkillDetail';
import ProjectCodeWorkflowDetail from '@/pages/platform/code-workflow/ProjectCodeWorkflowDetail';
import {DownloadIcon, ExternalLinkIcon, PanelRightCloseIcon, PlusIcon, XIcon} from 'lucide-react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

// Extensions whose files are editable text (code/markup/data). Only these get the Editor / Split view
// modes — every other file (images, PDFs, slide decks, binaries) can only be previewed, so the toggle is
// reduced to a single "Preview" for them. Mirrors AiHubFileViewer, which already forces Preview for any
// non-textual mime type regardless of the stored view mode.
const TEXTUAL_FILE_EXTENSIONS = new Set([
    'css',
    'csv',
    'htm',
    'html',
    'java',
    'js',
    'json',
    'jsx',
    'md',
    'markdown',
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

const renderTabBody = (tab: AiHubTabType) => {
    if (tab.kind === 'file') {
        return <AiHubFileViewer fileId={tab.fileId} name={tab.name} viewMode={tab.viewMode} />;
    }

    if (tab.kind === 'workflow') {
        return (
            <AiHubWorkflowViewer name={tab.name} projectId={tab.projectId} projectWorkflowId={tab.projectWorkflowId} />
        );
    }

    if (tab.kind === 'dataTable') {
        return <AiHubDataTableViewer dataTableId={tab.dataTableId} name={tab.name} />;
    }

    if (tab.kind === 'knowledgeBase') {
        return <AiHubKnowledgeBaseViewer knowledgeBaseId={tab.knowledgeBaseId} name={tab.name} />;
    }

    if (tab.kind === 'skill') {
        return <AiSkillDetail skillId={tab.skillId} />;
    }

    if (tab.kind === 'customComponent') {
        return <CustomComponentDetail customComponentId={tab.customComponentId} />;
    }

    if (tab.kind === 'codeWorkflow') {
        return <ProjectCodeWorkflowDetail language={tab.language} projectId={tab.projectId} />;
    }

    if (tab.kind === 'workflowExecution') {
        return <AiHubWorkflowExecutionViewer workflowExecutionId={tab.workflowExecutionId} />;
    }

    return null;
};

const AiHubResourcePanel = () => {
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

    return (
        // Island layout: a `surface-main` gutter (matching the page background) frames a rounded,
        // bordered card so the panel reads as a floating "island" detached from the window edges
        // (see AiHub.tsx for the matching invisible resize gap on the left). `overflow-hidden` clips
        // the document preview and tab-strip border to the rounded corners. The card uses
        // `surface-neutral-primary` (white in light mode; elevated dark in dark mode) so it lifts off
        // the gutter, and the plain `border` token — unlike `stroke-neutral-*` — adapts to dark mode.
        <div className="size-full bg-surface-main py-2 pr-2">
            <div className="flex size-full flex-col overflow-hidden rounded-xl border bg-surface-neutral-primary shadow-sm">
                {/* Padding mirrors AiHubPanel header (`px-4 py-3`) so this toolbar lines up vertically
                 * with the task title + Ask/Build group on the left side when the panel is open. The
                 * old `px-2 py-1` made the right side sit a few pixels higher than the left, with mismatched
                 * horizontal margins, breaking the visual baseline between the two sibling panel headers. */}

                {/*
                 * Tab strip styled to match WorkflowNodeDetailsPanel: underline-bottom-border on the active tab
                 * (border-stroke-brand-primary + brand-primary text), neutral muted text on inactive tabs. Each
                 * tab still carries an inline close X. The strip sits flush with the panel's top border so the
                 * underline reads as a continuous boundary between the tabs and the body below.
                 *
                 * `mt-3` pushes the whole strip down so its bottom edge lines up with the
                 * AiHubPanel header's bottom edge (the left panel header is `px-4 py-3` ≈ 60px tall;
                 * this strip is ~48px tall on its own — adding mt-3 to its top closes the 12px gap without
                 * making the strip itself any taller, keeping the original compact height the user prefers).
                 *
                 * `min-h-12` keeps the strip 48px when zero tabs are open (the picker + close icons alone
                 * would otherwise collapse it to ≈36px).
                 */}

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
                                >
                                    {tab.kind === 'workflow' ? (
                                        <WorkflowTabLabel
                                            name={tab.name}
                                            onClick={() => setActiveTab(tab.id)}
                                            projectId={tab.projectId}
                                        />
                                    ) : (
                                        <button
                                            className="max-w-40 truncate"
                                            onClick={() => setActiveTab(tab.id)}
                                            title={tab.name}
                                            type="button"
                                        >
                                            {tab.name}
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

                    {/* Close affordance + file picker live in their own padded slot so the underline border on
                     * the active tab doesn't run under them. */}

                    <div className="flex shrink-0 items-center gap-1 px-4">
                        <AiHubFilePicker />

                        <Tooltip>
                            <TooltipTrigger asChild>
                                <Button
                                    aria-label="Close resource panel"
                                    icon={<PanelRightCloseIcon />}
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

                                {/* Two file-level affordances next to the view-mode toggle:
                                 *
                                 *   - Open externally → deep-link to the asset-files admin page so the user can
                                 *     manage tags, rename, see usage, etc., outside of the task context.
                                 *     Routed at `/automation/asset-files/:fileId` (added in routes.tsx); the
                                 *     destination page reads the param and pops the existing detail sheet.
                                 *
                                 *   - Download → uses the {@code download} attribute pointed at the asset-file
                                 *     content endpoint so the browser fetches binary directly and saves under
                                 *     the file's name; works for text and binary files alike.
                                 *
                                 * Plain styled anchors instead of {@code <Button asChild>} because the custom
                                 * `@/components/Button/Button` doesn't expose an `asChild` prop — its `size`
                                 * type only accepts text-button variants when type-checking against the
                                 * top-level Button component, even though the icon-button branch exists at
                                 * runtime. Using anchors directly with the same Tailwind classes the icon
                                 * Button would render keeps the styling consistent without fighting the type.
                                 */}

                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Link
                                            aria-label="Open in Asset Files"
                                            className="inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground"
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
                                            className="inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground"
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

                        <div className="min-h-0 flex-1">{renderTabBody(activeTab)}</div>
                    </>
                ) : (
                    <div className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
                        Click <PlusIcon className="mx-1 inline size-4" /> above to add a resource
                    </div>
                )}
            </div>
        </div>
    );
};

export default AiHubResourcePanel;
