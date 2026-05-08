import Button from '@/components/Button/Button';
import UnsavedChangesAlertDialog from '@/components/UnsavedChangesAlertDialog';
import {ButtonGroup, ButtonGroupSeparator} from '@/components/ui/button-group';
import {ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Sheet, SheetClose, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import WorkflowExecutionsTestOutput from '@/pages/platform/workflow-editor/components/WorkflowExecutionsTestOutput';
import WorkflowTestConfigurationDialog from '@/pages/platform/workflow-editor/components/WorkflowTestConfigurationDialog';
import useWorkflowCodeEditorSheet from '@/pages/platform/workflow-editor/hooks/useWorkflowCodeEditorSheet';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import {Workflow, WorkflowTestConfiguration} from '@/shared/middleware/platform/configuration';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {
    ChevronDownIcon,
    CodeXmlIcon,
    InfoIcon,
    PlayIcon,
    RefreshCwIcon,
    SaveIcon,
    Settings2Icon,
    SparklesIcon,
    SquareIcon,
    XIcon,
} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';
import {Suspense, lazy} from 'react';
import {twMerge} from 'tailwind-merge';

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
        errors,
        errorsAccordionOpen,
        handleCopilotClick,
        handleCopilotClose,
        handleDefinitionChange,
        handleOpenChange,
        handleRunClick,
        handleSaveClick,
        handleStopClick,
        handleUnsavedChangesAlertDialogClose,
        handleUnsavedChangesAlertDialogOpen,
        handleValidate,
        handleWorkflowTestConfigurationDialog,
        hasErrors,
        projectName,
        setErrorPanelRef,
        setErrorsAccordionOpen,
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
                className="bottom-4 right-4 top-3 flex h-auto w-[60%] flex-row gap-0 rounded-lg border border-stroke-neutral-secondary bg-surface-neutral-secondary p-0 sm:max-w-[90%]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <div className="flex min-w-0 flex-1 flex-col">
                    <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-stroke-neutral-primary bg-surface-neutral-primary p-3">
                        <div className="flex items-center gap-2">
                            <CodeXmlIcon />

                            <span className="flex items-center justify-center gap-1 text-sm font-semibold">
                                Edit
                                <span className="font-normal text-content-neutral-secondary">{projectName} /</span>

                                {workflow.label}
                            </span>
                        </div>

                        <div className="flex items-center gap-2">
                            <Tooltip>
                                <TooltipTrigger asChild>
                                    <Button
                                        disabled={testConfigurationDisabled}
                                        icon={<Settings2Icon />}
                                        label="Test Configuration"
                                        onClick={() => handleWorkflowTestConfigurationDialog(true)}
                                        variant="secondary"
                                    />
                                </TooltipTrigger>

                                <TooltipContent>Set the workflow test configuration</TooltipContent>
                            </Tooltip>

                            <ButtonGroup>
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <div>
                                            <Button
                                                className="rounded-r-none opacity-50"
                                                disabled={!dirty || hasErrors}
                                                icon={<SaveIcon />}
                                                onClick={() => handleSaveClick(workflow, definition)}
                                                size="icon"
                                                type="submit"
                                            />
                                        </div>
                                    </TooltipTrigger>

                                    <TooltipContent>
                                        {hasErrors ? 'Saving is disabled due to code errors.' : 'Save current workflow'}
                                    </TooltipContent>
                                </Tooltip>

                                <ButtonGroupSeparator className="bg-stroke-brand-secondary" />

                                {!workflowIsRunning && (
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <span tabIndex={0}>
                                                <Button
                                                    className="rounded-l-none"
                                                    disabled={runDisabled || dirty}
                                                    icon={<PlayIcon />}
                                                    label="Test"
                                                    onClick={handleRunClick}
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
                                        label="Stop"
                                        onClick={handleStopClick}
                                        variant="destructive"
                                    />
                                )}
                            </ButtonGroup>

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

                            <SheetClose asChild>
                                <Button
                                    className="[&_svg]:size-5"
                                    icon={<XIcon />}
                                    onClick={() => handleOpenChange(false)}
                                    size="icon"
                                    variant="ghost"
                                />
                            </SheetClose>
                        </div>
                    </header>

                    <div className="flex min-h-0 flex-1 rounded-lg bg-surface-neutral-secondary">
                        <ResizablePanelGroup
                            className="gap-3 rounded-lg bg-surface-neutral-secondary p-3"
                            orientation="vertical"
                        >
                            <ResizablePanel className="rounded-lg bg-surface-neutral-primary" defaultSize={750}>
                                <Suspense fallback={<MonacoEditorLoader />}>
                                    <MonacoEditor
                                        className="size-full py-3"
                                        defaultLanguage={workflow.format?.toLowerCase() ?? 'json'}
                                        onChange={(value) => handleDefinitionChange(value as string)}
                                        onMount={(editor) => editor.focus()}
                                        onValidate={handleValidate}
                                        options={{
                                            folding: true,
                                            foldingStrategy: 'indentation',
                                        }}
                                        value={definition}
                                    />
                                </Suspense>
                            </ResizablePanel>

                            {errors?.length > 0 && (
                                <ResizablePanel
                                    className="flex w-full cursor-pointer flex-col overflow-hidden rounded-lg border border-stroke-destructive-primary bg-surface-destructive-secondary transition-all"
                                    collapsedSize={42}
                                    collapsible
                                    defaultSize={42}
                                    onClick={() => setErrorsAccordionOpen(!errorsAccordionOpen)}
                                    panelRef={setErrorPanelRef}
                                >
                                    <div className="sticky left-0 right-0 flex w-auto items-center gap-2 px-3 py-2">
                                        <InfoIcon className="text-content-destructive-primary" />

                                        <span className="text-sm font-semibold">Errors ({errors.length})</span>

                                        <Button className="ml-auto" size="xxs" variant="link">
                                            Show all
                                            <ChevronDownIcon
                                                className={twMerge(
                                                    'transition-all',
                                                    errorsAccordionOpen && 'rotate-180'
                                                )}
                                            />
                                        </Button>
                                    </div>

                                    {errorsAccordionOpen && (
                                        <ScrollArea>
                                            <ul className="flex flex-col gap-2 px-3 py-2">
                                                {errors.map((error, index) => (
                                                    <li
                                                        className="gap-1.5 rounded-md bg-surface-neutral-primary px-3 py-1.5 text-sm"
                                                        key={`${error}_${index}`}
                                                    >
                                                        {error}
                                                    </li>
                                                ))}
                                            </ul>
                                        </ScrollArea>
                                    )}
                                </ResizablePanel>
                            )}

                            {(workflowIsRunning || workflowTestExecution) && (
                                <ResizablePanel className="rounded-lg bg-surface-neutral-primary" defaultSize={500}>
                                    {workflowIsRunning ? (
                                        <div className="flex size-full items-center justify-center gap-x-1 p-3 text-center">
                                            <span className="flex animate-spin text-gray-400">
                                                <RefreshCwIcon className="size-4" />
                                            </span>

                                            <span className="text-muted-foreground">Workflow is running...</span>
                                        </div>
                                    ) : (
                                        workflowTestExecution && (
                                            <WorkflowExecutionsTestOutput
                                                resizablePanelSize={400}
                                                workflowIsRunning={workflowIsRunning}
                                                workflowTestExecution={workflowTestExecution}
                                            />
                                        )
                                    )}
                                </ResizablePanel>
                            )}
                        </ResizablePanelGroup>
                    </div>
                </div>

                <CopilotPanel
                    className="h-full rounded-r-md border-l border-l-border/50"
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
