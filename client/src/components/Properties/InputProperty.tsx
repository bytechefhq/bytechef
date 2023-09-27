import {DataPillType} from '@/types/types';
import Input from 'components/Input/Input';
import MentionsInput from 'components/MentionsInput/MentionsInput';
import {ReactNode, useRef, useState} from 'react';

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
    defaultValue,
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

    const isNumericalInput =
        getInputType() === 'number' || type === 'INTEGER' || type === 'NUMBER';

    return (
        <>
            {!mention &&
                (isNumericalInput ? (
                    <Input
                        description={description}
                        error={error}
                        fieldsetClassName="flex-1 mb-0"
                        key={name}
                        label={label || name}
                        leadingIcon={leadingIcon}
                        name={name!}
                        onChange={({target}) => {
                            console.log('onchange');
                            const {value} = target;

                            const integerOnlyRegex = /^[0-9\b]+$/;

                            if (value === '' || integerOnlyRegex.test(value)) {
                                setIntegerValue(value);
                            }
                        }}
                        required={required}
                        title={type}
                        type={hidden ? 'hidden' : getInputType()}
                        value={integerValue}
                    />
                ) : (
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
                        title={type}
                        type={hidden ? 'hidden' : getInputType()}
                        ref={inputRef}
                    />
                ))}

            {mention &&
                !!dataPills?.length &&
                !!name &&
                (isNumericalInput ? (
                    <MentionsInput
                        controlType={controlType || getInputType()}
                        description={description}
                        data={dataPills}
                        label={label}
                        leadingIcon={leadingIcon}
                        onKeyPress={(event: KeyboardEvent) =>
                            event.key !== '{' && event.preventDefault()
                        }
                    />
                ) : (
                    <MentionsInput
                        controlType={controlType || getInputType()}
                        description={description}
                        data={dataPills}
                        label={label}
                        leadingIcon={leadingIcon}
                    />
                ))}
        </>
    );
};

export default InputProperty;
