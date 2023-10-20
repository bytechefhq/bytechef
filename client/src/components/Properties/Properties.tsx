import {PropertyModel} from '../../middleware/definition-registry';
import {UseFormRegister} from 'react-hook-form/dist/types/form';
import Input from '../Input/Input';

interface PropertiesProps<TProperty extends PropertyModel> {
    path?: string;
    properties?: TProperty[];
    /* eslint-disable @typescript-eslint/no-explicit-any */
    register: UseFormRegister<any>;
}

const Properties = <TProperty extends PropertyModel>({
    path = 'parameters',
    properties,
    register,
}: PropertiesProps<TProperty>): JSX.Element => {
    return (
        <>
            {properties &&
                properties.map((property) => {
                    if (property.type === 'STRING') {
                        return (
                            <Input
                                key={property.name}
                                label={property.label}
                                {...register(path + '.' + property.name!, {
                                    required: property.required!,
                                })}
                            />
                        );
                    }
                })}
        </>
    );
};

Properties.displayName = 'Properties';

export default Properties;
