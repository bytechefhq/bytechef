import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {CheckIcon} from 'lucide-react';
import {Select as SelectPrimitive} from 'radix-ui';

const ProjectDeploymentDialogBasicStepProjectVersionsSelect = ({
    onChange,
    projectId,
    projectVersion,
}: {
    onChange: (value: number) => void;
    projectId: number;
    projectVersion?: number;
}) => {
    const {data: projectVersions, isPending} = useGetProjectVersionsQuery(projectId);

    return (
        <Select
            disabled={isPending}
            onValueChange={(value) => onChange(+value)}
            value={projectVersion?.toString() || ''}
        >
            <SelectTrigger className="w-full">
                <SelectValue placeholder={isPending ? 'Loading versions…' : 'Select version'} />
            </SelectTrigger>

            <SelectContent>
                {isPending ? (
                    <span className="flex items-center gap-2 px-2 py-3 text-sm text-muted-foreground">
                        Loading versions…
                    </span>
                ) : (
                    projectVersions &&
                    projectVersions.map(
                        (projectVersion) =>
                            projectVersion.status == ProjectStatus.Published && (
                                <SelectPrimitive.Item
                                    className="radix-disabled:opacity-50 flex cursor-pointer items-center overflow-hidden rounded-md p-2 text-sm font-medium text-gray-700 select-none focus:bg-gray-100 focus:outline-hidden"
                                    key={projectVersion.version}
                                    value={projectVersion.version!.toString()}
                                >
                                    <span className="absolute right-2 flex size-3.5 items-center justify-center">
                                        <SelectPrimitive.ItemIndicator>
                                            <CheckIcon className="size-4" />
                                        </SelectPrimitive.ItemIndicator>
                                    </span>

                                    <div className="flex flex-col">
                                        <SelectPrimitive.ItemText>V{projectVersion.version}</SelectPrimitive.ItemText>

                                        <div className="max-w-96 text-xs text-muted-foreground">
                                            {projectVersion.description}
                                        </div>
                                    </div>
                                </SelectPrimitive.Item>
                            )
                    )
                )}
            </SelectContent>
        </Select>
    );
};

export default ProjectDeploymentDialogBasicStepProjectVersionsSelect;
