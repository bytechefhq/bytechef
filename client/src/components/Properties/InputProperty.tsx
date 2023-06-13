import Input from 'components/Input/Input';
import {ReactNode} from 'react';

interface InputPropertyProps {
    controlType?: string;
    description?: string;
    defaultValue?: string;
    error?: boolean;
    fieldsetClassName?: string;
    hidden?: boolean;
    name?: string;
    label?: string;
    leadingIcon?: ReactNode;
    required?: boolean;
    title?: string;
}

const InputProperty = ({
    controlType,
    defaultValue,
    description,
    error,
    hidden,
    label,
    leadingIcon,
    name,
    required,
    title,
}: InputPropertyProps) => {
    const getInputType = () => {
        switch (controlType) {
            case 'DATE':
                return 'date';
            case 'DATE_TIME':
                return 'datetime-local';
            case 'EMAIL':
                return 'email';
            case 'NUMBER':
                return 'number';
            case 'PASSWORD':
                return 'password';
            case 'PHONE':
                return 'tel';
            case 'TIME':
                return 'time';
            case 'URL':
                return 'url';
            default:
                return 'text';
        }
    };

    return (
        <Input
            description={description}
            defaultValue={defaultValue as string}
            error={error}
            fieldsetClassName="flex-1 mb-0"
            key={name}
            label={label || name}
            leadingIcon={leadingIcon}
            name={name!}
            required={required}
            title={title}
            type={hidden ? 'hidden' : getInputType()}
        />
    );
};

export default InputProperty;
