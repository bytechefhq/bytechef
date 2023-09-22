import {DataPillType} from '@/types/types';
import Input from 'components/Input/Input';
import MentionsInput from 'components/MentionsInput/MentionsInput';
import {ReactNode, useRef} from 'react';

interface InputPropertyProps {
    controlType?: string;
    dataPills?: DataPillType[];
    description?: string;
    defaultValue?: string;
    error?: boolean;
    fieldsetClassName?: string;
    hidden?: boolean;
    mention?: boolean;
    name?: string;
    label?: string;
    leadingIcon?: ReactNode;
    required?: boolean;
    title?: string;
}

const InputProperty = ({
    controlType,
    dataPills,
    defaultValue,
    description,
    error,
    hidden,
    label,
    leadingIcon,
    mention,
    name,
    required,
    title,
}: InputPropertyProps) => {
    const inputRef = useRef(null);

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
        <>
            {mention && !!dataPills?.length && !!name && (
                <MentionsInput
                    data={dataPills}
                    label={label}
                    leadingIcon={leadingIcon}
                />
            )}

            {!dataPills?.length && (
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
                    ref={inputRef}
                />
            )}
        </>
    );
};

export default InputProperty;
