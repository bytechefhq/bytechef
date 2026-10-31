import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {
    AutomationWorkflowProjectVersionsQuery,
    useAutomationWorkflowProjectVersionsQuery,
} from '@/shared/middleware/graphql';
import {VisuallyHidden} from 'radix-ui';
import {useState} from 'react';

import AutomationWorkflowProjectVersionWorkflowChanges from './AutomationWorkflowProjectVersionWorkflowChanges';

interface AutomationWorkflowProjectVersionHistorySheetPropsI {
    onClose: () => void;
    open: boolean;
    projectId: string;
}

interface AutomationWorkflowProjectVersionAccordionProps {
    projectId: string;
    projectVersions: AutomationWorkflowProjectVersionsQuery['automationWorkflowProjectVersions'];
}

const AutomationWorkflowProjectVersionAccordion = ({
    projectId,
    projectVersions,
}: AutomationWorkflowProjectVersionAccordionProps) => {
    const [openValues, setOpenValues] = useState<string[]>(
        projectVersions.length > 1 ? [projectVersions[1]!.version.toString()] : []
    );

    return (
        <Accordion onValueChange={setOpenValues} type="multiple" value={openValues}>
            {projectVersions.map((projectVersion) => (
                <AccordionItem key={projectVersion.version} value={projectVersion.version.toString()}>
                    <AccordionTrigger>
                        <div className="flex w-full items-center justify-between pr-2">
                            <span className="text-sm font-semibold">{`V${projectVersion.version}`}</span>

                            <div className="flex items-center space-x-4">
                                {projectVersion.publishedDate && (
                                    <span className="text-sm">
                                        {new Date(projectVersion.publishedDate).toLocaleString()}
                                    </span>
                                )}

                                <Badge
                                    className="border-transparent"
                                    label={projectVersion.status}
                                    styleType={
                                        projectVersion.status === 'PUBLISHED' ? 'success-outline' : 'secondary-filled'
                                    }
                                    weight="semibold"
                                />
                            </div>
                        </div>
                    </AccordionTrigger>

                    <AccordionContent className="text-muted-foreground">
                        <p>
                            {projectVersion.description
                                ? projectVersion.description.split('\n').map((line, index) => (
                                      <span key={index}>
                                          {line}

                                          <br />
                                      </span>
                                  ))
                                : 'No description.'}
                        </p>

                        <AutomationWorkflowProjectVersionWorkflowChanges
                            draft={projectVersion.status === 'DRAFT'}
                            expanded={openValues.includes(projectVersion.version.toString())}
                            projectId={projectId}
                            projectVersion={projectVersion.version}
                        />
                    </AccordionContent>
                </AccordionItem>
            ))}
        </Accordion>
    );
};

const AutomationWorkflowProjectVersionHistorySheet = ({
    onClose,
    open,
    projectId,
}: AutomationWorkflowProjectVersionHistorySheetPropsI) => {
    const {data: versions} = useAutomationWorkflowProjectVersionsQuery({id: projectId}, {enabled: open});

    const projectVersions = versions?.automationWorkflowProjectVersions;

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

                <div className="m-3 flex min-h-0 flex-1 flex-col overflow-y-auto rounded-lg bg-surface-neutral-primary p-3">
                    {projectVersions && (
                        <AutomationWorkflowProjectVersionAccordion
                            projectId={projectId}
                            projectVersions={projectVersions}
                        />
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default AutomationWorkflowProjectVersionHistorySheet;
