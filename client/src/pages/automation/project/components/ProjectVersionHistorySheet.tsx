import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import {Badge} from '@/components/ui/badge';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {ProjectStatusModel} from '@/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/queries/automation/projectVersions.queries';
import * as SheetPrimitive from '@radix-ui/react-dialog';
import {Cross2Icon} from '@radix-ui/react-icons';

interface ProjectVersionHistorySheetProps {
    onClose: () => void;
    projectId: number;
}

const ProjectVersionHistorySheet = ({onClose, projectId}: ProjectVersionHistorySheetProps) => {
    const {data: projectVersions} = useGetProjectVersionsQuery(projectId);

    return (
        <Sheet onOpenChange={() => onClose()} open>
            <SheetContent
                className="flex flex-col p-4 sm:max-w-[500px]"
                onFocusOutside={(event) => event.preventDefault()}
                onPointerDownOutside={(event) => event.preventDefault()}
            >
                <SheetHeader>
                    <SheetTitle>
                        <div className="flex items-center justify-between">
                            <span>Project Version History</span>

                            <SheetPrimitive.Close asChild>
                                <Cross2Icon className="size-4 cursor-pointer opacity-70" />
                            </SheetPrimitive.Close>
                        </div>
                    </SheetTitle>
                </SheetHeader>

                <div className="overflow-y-auto">
                    <Accordion type="single">
                        {projectVersions &&
                            projectVersions.map((projectVersion) => (
                                <AccordionItem
                                    key={projectVersion.version}
                                    value={projectVersion.version?.toString() || ''}
                                >
                                    <AccordionTrigger disabled={projectVersion.status === ProjectStatusModel.Draft}>
                                        <div className="flex w-full items-center justify-between pr-2">
                                            <span className="text-sm font-semibold">{`V${projectVersion.version}`}</span>

                                            <div className="flex items-center space-x-4">
                                                {projectVersion.publishedDate && (
                                                    <span className="text-sm">
                                                        {`${projectVersion.publishedDate?.toLocaleDateString()} ${projectVersion.publishedDate?.toLocaleTimeString()}`}
                                                    </span>
                                                )}

                                                <Badge
                                                    variant={
                                                        projectVersion.status === ProjectStatusModel.Published
                                                            ? 'success'
                                                            : 'secondary'
                                                    }
                                                >
                                                    {projectVersion.status === ProjectStatusModel.Published
                                                        ? `Published`
                                                        : 'Draft'}
                                                </Badge>
                                            </div>
                                        </div>
                                    </AccordionTrigger>

                                    <AccordionContent>{projectVersion.description}</AccordionContent>
                                </AccordionItem>
                            ))}
                    </Accordion>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default ProjectVersionHistorySheet;
