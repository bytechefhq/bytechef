import CreatableSelect from '@/components/CreatableSelect/CreatableSelect';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useFormContext} from 'react-hook-form';

interface TagsFormFieldProps {
    remainingTags?: {name: string}[];
}

const TagsFormField = ({remainingTags}: TagsFormFieldProps) => {
    const {control, getValues, setValue} = useFormContext();

    if (!remainingTags) {
        return null;
    }

    return (
        <FormField
            control={control}
            name="tags"
            render={({field}) => (
                <FormItem>
                    <FormLabel>Tags</FormLabel>

                    <FormControl>
                        <CreatableSelect
                            field={field}
                            isMulti
                            onCreateOption={(inputValue: string) => {
                                setValue('tags', [
                                    ...(getValues().tags || []),
                                    {
                                        label: inputValue,
                                        name: inputValue,
                                        value: inputValue,
                                    },
                                ]);
                            }}
                            options={remainingTags.map((tag) => ({
                                label: tag.name,
                                value: tag.name.toLowerCase().replace(/\W/g, ''),
                                ...tag,
                            }))}
                        />
                    </FormControl>

                    <FormMessage />
                </FormItem>
            )}
        />
    );
};

export default TagsFormField;
