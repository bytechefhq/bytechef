import Button from '@/components/Button/Button';
import UnsavedChangesAlertDialog from '@/components/UnsavedChangesAlertDialog';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestConfigurationDialog from '@/pages/platform/workflow-editor/components/WorkflowTestConfigurationDialog';
import useWorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {PlayIcon, RefreshCwIcon, SaveIcon, Settings2Icon, SparklesIcon, SquareIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy} from 'react';

interface WorkflowCodeEditorSheetProps {
    invalidateWorkflowQueries: () => void;
    onSheetOpenClose: (open: boolean) => void;
    runDisabled: boolean;
    sheetOpen: boolean;
    testConfigurationDisabled: boolean;
    workflow: Workflow;
    workflowTestConfiguration?: WorkflowTestConfiguration;
}

const MonacoEditor = lazy(() => import('@/shared/components/MonacoEditorWrapper'));

const WorkflowCodeEditorSheet = ({
    invalidateWorkflowQueries,
    onSheetOpenClose,
    runDisabled,
    sheetOpen,
    testConfigurationDisabled,
    workflow,
    workflowTestConfiguration,
}: WorkflowCodeEditorSheetProps) => {
    const {
        copilotEnabled,
        copilotPanelOpen,
        definition,
        dirty,
        handleCopilotClick,
        handleCopilotClose,
        handleDefinitionChange,
        handleOpenChange,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        handleUnsavedChangesAlertDialogClose,
        handleUnsavedChangesAlertDialogOpen,
        handleWorkflowTestConfigurationDialog,
        showWorkflowTestConfigurationDialog,
        unsavedChangesAlertDialogOpen,
        workflowIsRunning,
        workflowTestExecution,
    } = useWorkflowCodeEditorSheet({invalidateWorkflowQueries, onSheetOpenClose, workflow});

    const ff_4076 = useFeatureFlagsStore()('ff-4076');

    return (
        <Sheet onOpenChange={handleOpenChange} open={sheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>Edit Workflow</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="absolute bottom-4 right-4 top-3 flex h-auto w-[90%] flex-row gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <div className="flex min-w-0 flex-1 flex-col">
                    <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                        <span className="text-lg font-semibold">Edit Workflow</span>

                        <div className="flex items-center gap-1">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={testConfigurationDisabled}
                                        icon={<Settings2Icon />}
                                        onClick={() => handleWorkflowTestConfigurationDialog(true)}
                                        size="icon"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Set the workflow test configuration</TooltipContent>
                            </Tooltip>

                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={!dirty}
                                        icon={<SaveIcon />}
                                        onClick={() => handleSaveClick(workflow, definition)}
                                        size="icon"
                                        type="submit"
                                        variant="ghost"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Save current workflow</TooltipContent>
                            </Tooltip>

                            {!workflowIsRunning && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span tabIndex={0}>
                                            <Button
                                                disabled={runDisabled || dirty}
                                                icon={<PlayIcon className="text-success" />}
                                                onClick={handleRunClick}
                                                size="icon"
                                                variant="ghost"
                                            />
                                        </span>
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        {runDisabled
                                            ? `The workflow cannot be executed. Please set all required workflow input parameters, connections and component properties.`
                                            : `Run the current workflow`}
                                    </TooltipContent>
                                </Tooltip>
                            )}

                            {workflowIsRunning && (
                                <Button
                                    icon={<SquareIcon />}
                                    onClick={handleStopClick}
                                    size="icon"
                                    variant="destructive"
                                />
                            )}

                            {ff_4076 && copilotEnabled && (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <Button
                                            className="[&_svg]:size-5"
                                            icon={<SparklesIcon />}
                                            onClick={handleCopilotClick}
                                            size="icon"
                                            variant="ghost"
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent>Open Copilot panel</TooltipContent>
                                </Tooltip>
                            )}

                            <SheetCloseButton />
                        </div>
                    </header>

                    <div className="flex min-h-0 flex-1">
                        <ResizablePanelGroup
                            className="flex-1 rounded-md bg-surface-neutral-primary"
                            direction="vertical"
                        >
                            <ResizablePanel defaultSize={75}>
                                <Suspense fallback={<MonacoEditorLoader />}>
                                    <MonacoEditor
                                        className="size-full"
                                        defaultLanguage={workflow.format?.toLowerCase() ?? 'json'}
                                        onChange={(value) => handleDefinitionChange(value as string)}
                                        onMount={(editor) => editor.focus()}
                                        options={{
                                            folding: true,
                                            foldingStrategy: 'indentation',
                                        }}
                                        value={definition}
                                    />
                                </Suspense>
                            </ResizablePanel>

                            <ResizableHandle className="bg-muted" />

                            <ResizablePanel defaultSize={25}>
                                {workflowIsRunning ? (
                                    <div className="flex items-center gap-x-1 p-3">
                                        <span className="flex animate-spin text-gray-400">
                                            <RefreshCwIcon className="size-4" />
                                        </span>

                                        <span className="text-muted-foreground">Workflow is running...</span>
                                    </div>
                                ) : (
                                    <WorkflowExecutionsTestOutput
                                        resizablePanelSize={40}
                                        workflowIsRunning={workflowIsRunning}
                                        workflowTestExecution={workflowTestExecution}
                                    />
                                )}
                            </ResizablePanel>
                        </ResizablePanelGroup>
                    </div>
                </div>

                <CopilotPanel
                    className="h-full rounded-r-md border-l"
                    onClose={handleCopilotClose}
                    open={copilotPanelOpen}
                />
            </SheetContent>

            <UnsavedChangesAlertDialog
                onCancel={() => handleUnsavedChangesAlertDialogOpen(false)}
                onClose={handleUnsavedChangesAlertDialogClose}
                open={unsavedChangesAlertDialogOpen}
            />

            {showWorkflowTestConfigurationDialog && (
                <WorkflowTestConfigurationDialog
                    onClose={() => handleWorkflowTestConfigurationDialog(false)}
                    workflow={workflow}
                    workflowTestConfiguration={workflowTestConfiguration}
                />
            )}
        </Sheet>
    );
};

export default WorkflowCodeEditorSheet;
