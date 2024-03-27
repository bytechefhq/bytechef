import {Badge} from '@/components/ui/badge';
import {Sheet, SheetContent, SheetHeader, SheetTitle} from '@/components/ui/sheet';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
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

                <ul className="flex flex-col divide-y divide-gray-100 overflow-y-auto">
                    {projectVersions &&
                        projectVersions.map((projectVersion) => (
                            <li
                                className="flex w-full cursor-pointer justify-between py-4"
                                key={projectVersion.version}
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <span className="text-sm font-semibold">{`V${projectVersion.version}`}</span>
                                    </TooltipTrigger>

                                    {projectVersion.description && (
                                        <TooltipContent side="right">{projectVersion.description}</TooltipContent>
                                    )}
                                </Tooltip>

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
                                        {projectVersion.status === ProjectStatusModel.Published ? `Published` : 'Draft'}
                                    </Badge>
                                </div>
                            </li>
                        ))}
                </ul>
            </SheetContent>
        </Sheet>
    );
};

export default ProjectVersionHistorySheet;
