import {Select, SelectContent, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';
import {CheckIcon} from 'lucide-react';
import {Select as SelectPrimitive} from 'radix-ui';
import {useMemo} from 'react';

const ProjectDeploymentDialogBasicStepProjectVersionsSelect = ({
    onChange,
    projectId,
    projectVersion,
}: {
    onChange: (value: number) => void;
    projectId: number;
    projectVersion?: number;
}) => {
    const {data: projectVersions, isError, isPending} = useGetProjectVersionsQuery(projectId);

    const publishedProjectVersions = useMemo(
        () => (projectVersions ?? []).filter((projectVersion) => projectVersion.status === ProjectStatus.Published),
        [projectVersions]
    );

    if (isError) {
        return <p className="text-sm text-destructive">Could not load the project versions. Try again later.</p>;
    }

    if (!isPending && publishedProjectVersions.length === 0) {
        return (
            <p className="text-sm text-muted-foreground">
                This project has no published version yet. Publish it first.
            </p>
        );
    }

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
                    publishedProjectVersions.map((projectVersion) => (
                        <SelectPrimitive.Item
                            className="radix-disabled:opacity-50 flex cursor-pointer items-center overflow-hidden rounded-md p-2 text-sm font-medium text-content-neutral-secondary select-none focus:bg-surface-neutral-primary-hover focus:outline-hidden"
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
                    ))
                )}
            </SelectContent>
        </Select>
    );
};

export default ProjectDeploymentDialogBasicStepProjectVersionsSelect;
