import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle} from '@/components/ui/sheet';
import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';
import {VisuallyHidden} from 'radix-ui';

interface ProjectVersionHistorySheetProps {
    onSheetOpenChange: (open: boolean) => void;
    projectVersions: ProjectVersion[];
    sheetOpen: boolean;
}

const ProjectVersionHistorySheet = ({
    onSheetOpenChange,
    projectVersions,
    sheetOpen,
}: ProjectVersionHistorySheetProps) => {
    return (
        <Sheet onOpenChange={onSheetOpenChange} open={sheetOpen}>
            <VisuallyHidden.Root>
                <SheetTitle>Project Version History</SheetTitle>
            </VisuallyHidden.Root>

            <SheetContent
                className="bottom-4 right-4 top-3 flex h-auto flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md border-b border-b-border/50 bg-surface-neutral-primary p-3">
                    <span className="text-lg font-semibold">Project Version History</span>

                    <SheetCloseButton />
                </header>

                <div className="flex min-h-0 flex-1 flex-col overflow-y-auto p-3">
                    {projectVersions && (
                        <Accordion
                            defaultValue={projectVersions.length > 1 ? [projectVersions[1]!.version!.toString()] : []}
                            type="multiple"
                        >
                            {projectVersions.map((projectVersion) => {
                                return (
                                    <AccordionItem
                                        key={projectVersion.version}
                                        value={projectVersion.version?.toString() || ''}
                                    >
                                        <AccordionTrigger disabled={projectVersion.status === ProjectStatus.Draft}>
                                            <div className="flex w-full items-center justify-between pr-2">
                                                <span className="text-sm font-semibold">{`V${projectVersion.version}`}</span>

                                                <div className="flex items-center space-x-4">
                                                    {projectVersion.publishedDate && (
                                                        <span className="text-sm">
                                                            {`${projectVersion.publishedDate?.toLocaleDateString()} ${projectVersion.publishedDate?.toLocaleTimeString()}`}
                                                        </span>
                                                    )}

                                                    <Badge
                                                        label={
                                                            projectVersion.status === ProjectStatus.Published
                                                                ? 'Published'
                                                                : 'Draft'
                                                        }
                                                        styleType={
                                                            projectVersion.status === ProjectStatus.Published
                                                                ? 'success-outline'
                                                                : 'secondary-filled'
                                                        }
                                                        weight="semibold"
                                                    />
                                                </div>
                                            </div>
                                        </AccordionTrigger>

                                        <AccordionContent className="text-muted-foreground">
                                            {projectVersion.description
                                                ? projectVersion.description.split('\n').map((item, idx) => (
                                                      <span key={idx}>
                                                          {item}

                                                          <br />
                                                      </span>
                                                  ))
                                                : 'No description.'}
                                        </AccordionContent>
                                    </AccordionItem>
                                );
                            })}
                        </Accordion>
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default ProjectVersionHistorySheet;
