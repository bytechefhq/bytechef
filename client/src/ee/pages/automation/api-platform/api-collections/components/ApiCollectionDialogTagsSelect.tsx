import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {useGetApiCollectionTagsQuery} from '@/ee/queries/apiCollectionTags.queries';
import {ApiCollection} from '@/ee/shared/middleware/automation/api-platform';
import {ControllerRenderProps} from 'react-hook-form/dist/types/controller';

const ApiCollectionDialogTagsSelect = ({
    apiCollection,
    field,
    onCreateOption,
}: {
    field: ControllerRenderProps<ApiCollection, 'tags'>;
    onCreateOption: (inputValue: string) => void;
    apiCollection?: ApiCollection;
}) => {
    const {data: tags} = useGetApiCollectionTagsQuery();

    const tagNames = apiCollection?.tags?.map((tag) => tag.name);

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

export default ApiCollectionDialogTagsSelect;
