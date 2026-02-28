import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {Item, ItemIndicator, ItemText} from '@radix-ui/react-select';
import {CheckIcon} from 'lucide-react';

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
                                <Item
                                    className="radix-disabled:opacity-50 flex cursor-pointer select-none items-center overflow-hidden rounded-md p-2 text-sm font-medium text-gray-700 focus:bg-gray-100 focus:outline-none"
                                    key={projectVersion.version}
                                    value={projectVersion.version!.toString()}
                                >
                                    <span className="absolute right-2 flex size-3.5 items-center justify-center">
                                        <ItemIndicator>
                                            <CheckIcon className="size-4" />
                                        </ItemIndicator>
                                    </span>

                                    <div className="flex flex-col">
                                        <ItemText>V{projectVersion.version}</ItemText>

                                        <div className="max-w-96 text-xs text-muted-foreground">
                                            {projectVersion.description}
                                        </div>
                                    </div>
                                </Item>
                            )
                    )
                )}
            </SelectContent>
        </Select>
    );
};

export default ProjectDeploymentDialogBasicStepProjectVersionsSelect;
