import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {useGetProjectDeploymentTagsQuery} from '@/shared/queries/automation/projectDeploymentTags.queries';
import {ControllerRenderProps} from 'react-hook-form';

const ProjectDeploymentDialogBasicStepTagsSelect = ({
    field,
    onCreateOption,
    projectDeployment,
}: {
    field: ControllerRenderProps<ProjectDeployment, 'tags'>;
    onCreateOption: (inputValue: string) => void;
    projectDeployment?: ProjectDeployment;
}) => {
    const {data: tags} = useGetProjectDeploymentTagsQuery();

    const tagNames = projectDeployment?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    return remainingTags ? (
        <CreatableSelect
            field={field}
            isMulti
            onCreateOption={onCreateOption}
            options={remainingTags.map((tag) => {
                return {
                    label: tag.name,
                    value: tag.name.toLowerCase().replace(/\W/g, ''),
                    ...tag,
                };
            })}
        />
    ) : (
        <>Loading....</>
    );
};

export default ProjectDeploymentDialogBasicStepTagsSelect;
