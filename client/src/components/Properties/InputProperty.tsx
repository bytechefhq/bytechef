import {DataPillType} from '@/types/types';
import Input from 'components/Input/Input';
import MentionsInput from 'components/MentionsInput/MentionsInput';
import {ReactNode, useState} from 'react';

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
    type?: string;
}

const InputProperty = ({
    controlType,
    dataPills,
    description,
    error,
    hidden,
    label,
    leadingIcon,
    mention,
    name,
    required,
    type,
}: InputPropertyProps) => {
    const [integerValue, setIntegerValue] = useState('');

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

    const isNumericalInput =
        getInputType() === 'number' || type === 'INTEGER' || type === 'NUMBER';

    return (
        <>
            {!mention && (
                <Input
                    description={description}
                    error={error}
                    fieldsetClassName="flex-1 mb-0"
                    key={name}
                    label={label || name}
                    leadingIcon={leadingIcon}
                    name={name!}
                    onChange={(event) => {
                        if (isNumericalInput) {
                            const {value} = event.target;

                            const integerOnlyRegex = /^[0-9\b]+$/;

                            if (value === '' || integerOnlyRegex.test(value)) {
                                setIntegerValue(value);
                            }
                        }
                    }}
                    required={required}
                    title={type}
                    type={hidden ? 'hidden' : getInputType()}
                    value={isNumericalInput ? integerValue : undefined}
                />
            )}

            {mention && !!dataPills?.length && !!name && (
                <MentionsInput
                    controlType={controlType || getInputType()}
                    description={description}
                    data={dataPills}
                    label={label}
                    leadingIcon={leadingIcon}
                    onKeyPress={(event: KeyboardEvent) => {
                        if (isNumericalInput) {
                            event.key !== '{' && event.preventDefault();
                        }
                    }}
                />
            )}
        </>
    );
};

export default InputProperty;
