import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {ProjectInstance} from '@/shared/middleware/automation/configuration';
import {useGetProjectInstanceTagsQuery} from '@/shared/queries/automation/projectInstanceTags.queries';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';

const ProjectInstanceDialogBasicStepTagsSelect = ({
    field,
    onCreateOption,
    projectInstance,
}: {
    field: ControllerRenderProps<ProjectInstance, 'tags'>;
    onCreateOption: (inputValue: string) => void;
    projectInstance?: ProjectInstance;
}) => {
    const {data: tags} = useGetProjectInstanceTagsQuery();

    const tagNames = projectInstance?.tags?.map((tag) => tag.name);

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

export default ProjectInstanceDialogBasicStepTagsSelect;
