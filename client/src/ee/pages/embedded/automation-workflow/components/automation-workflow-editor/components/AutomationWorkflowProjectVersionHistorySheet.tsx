import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {useAutomationWorkflowProjectVersionsQuery} from '@/shared/middleware/graphql';
import {VisuallyHidden} from 'radix-ui';

interface AutomationWorkflowProjectVersionHistorySheetPropsI {
    onClose: () => void;
    open: boolean;
    projectId: string;
}

const AutomationWorkflowProjectVersionHistorySheet = ({
    onClose,
    open,
    projectId,
}: AutomationWorkflowProjectVersionHistorySheetPropsI) => {
    const {data: versions} = useAutomationWorkflowProjectVersionsQuery({id: projectId}, {enabled: open});

    const projectVersions = versions?.automationWorkflowProjectVersions ?? [];

    return (
        <Sheet onOpenChange={(value) => !value && onClose()} open={open}>
            <VisuallyHidden.Root>
                <SheetTitle>Project Version History</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="top-3 right-4 bottom-4 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <span className="text-lg font-semibold">Project Version History</span>

                    <SheetCloseButton />
                </header>

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-3">
                    <Accordion
                        defaultValue={projectVersions.length > 1 ? [projectVersions[1]!.version.toString()] : []}
                        type="multiple"
                    >
                        {projectVersions.map((projectVersion) => (
                            <AccordionItem key={projectVersion.version} value={projectVersion.version.toString()}>
                                <AccordionTrigger disabled={projectVersion.status === 'DRAFT'}>
                                    <div className="flex w-full items-center justify-between pr-2">
                                        <span className="text-sm font-semibold">{`V${projectVersion.version}`}</span>

                                        <div className="flex items-center space-x-4">
                                            {projectVersion.publishedDate && (
                                                <span className="text-sm">
                                                    {new Date(projectVersion.publishedDate).toLocaleString()}
                                                </span>
                                            )}

                                            <Badge
                                                label={projectVersion.status}
                                                styleType={
                                                    projectVersion.status === 'PUBLISHED'
                                                        ? 'success-outline'
                                                        : 'secondary-filled'
                                                }
                                                weight="semibold"
                                            />
                                        </div>
                                    </div>
                                </AccordionTrigger>

                                <AccordionContent className="text-muted-foreground">No description.</AccordionContent>
                            </AccordionItem>
                        ))}
                    </Accordion>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default AutomationWorkflowProjectVersionHistorySheet;
