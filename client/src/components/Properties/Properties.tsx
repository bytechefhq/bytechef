import {PropertyModel} from '../../middleware/definition-registry';
import {FormState, UseFormRegister} from 'react-hook-form/dist/types/form';
import Input from '../Input/Input';
import {Path, FieldValues} from 'react-hook-form/dist/types';

interface PropertiesProps<
    TProperty extends PropertyModel,
    TFieldValues extends FieldValues = FieldValues
> {
    formState: FormState<TFieldValues>;
    path?: string;
    properties?: TProperty[];
    register: UseFormRegister<TFieldValues>;
}

const Properties = <
    TProperty extends PropertyModel,
    TFieldValues extends FieldValues = FieldValues
>({
    formState: {errors, touchedFields},
    path = 'parameters',
    properties,
    register,
}: PropertiesProps<TProperty, TFieldValues>): JSX.Element => {
    function isError(propertyName: string) {
        if (
            touchedFields[path] &&
            touchedFields[path]![propertyName] &&
            errors[path] &&
            (errors[path] as never)[propertyName]
        ) {
            return true;
        }

        return false;
    }

    return (
        <>
            {properties &&
                properties.map((property) => {
                    if (property.type === 'STRING') {
                        return (
                            <Input
                                key={property.name}
                                error={isError(property.name!)}
                                label={property.label}
                                {...register(
                                    (path +
                                        '.' +
                                        property.name!) as Path<TFieldValues>,
                                    {
                                        required: property.required!,
                                    }
                                )}
                            />
                        );
                    }
                })}
        </>
    );
};

Properties.displayName = 'Properties';

export default Properties;
