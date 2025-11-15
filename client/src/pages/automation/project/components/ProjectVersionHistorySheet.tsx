import Badge from '@/components/Badge/Badge';
import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {ProjectStatus, ProjectVersion} from '@/shared/middleware/automation/configuration';

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
            <SheetContent
                className="flex flex-col p-4 sm:max-w-workflow-sidebar-project-version-history-sheet-width"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                    <SheetTitle>Project Version History</SheetTitle>

                    <SheetCloseButton />
                </SheetHeader>

                <div className="overflow-y-auto">
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
