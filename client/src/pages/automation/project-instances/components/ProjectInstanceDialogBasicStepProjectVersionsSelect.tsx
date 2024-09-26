import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {useGetProjectVersionsQuery} from '@/shared/queries/automation/projectVersions.queries';

const ProjectInstanceDialogBasicStepProjectVersionsSelect = ({
    onChange,
    projectId,
    projectVersion,
}: {
    onChange: (value: number) => void;
    projectId: number;
    projectVersion?: number;
}) => {
    const {data: projectVersions} = useGetProjectVersionsQuery(projectId);

    return (
        <Select
            onValueChange={(value) => {
                onChange(+value);
            }}
            value={projectVersion?.toString() || ''}
        >
            <SelectTrigger className="w-full">
                <SelectValue placeholder="Select version" />
            </SelectTrigger>

            <SelectContent>
                {projectVersions &&
                    projectVersions.map(
                        (projectVersion) =>
                            projectVersion.status == ProjectStatus.Published && (
                                <SelectItem key={projectVersion.version} value={projectVersion.version!.toString()}>
                                    V{projectVersion.version}
                                </SelectItem>
                            )
                    )}
            </SelectContent>
        </Select>
    );
};

export default ProjectInstanceDialogBasicStepProjectVersionsSelect;
