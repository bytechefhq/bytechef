import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import React, {useEffect, useState} from 'react';

const handleEnterPress = (handler: () => void) => {
    return (event: React.KeyboardEvent<HTMLInputElement>): void => {
        if (event.key === 'Enter') {
            handler();
        }
    };
};

interface SchemaInputProps {
    autoFocus?: boolean;
    onChange: (text: string) => void;
    placeholder?: string;
    value?: string;
    label?: string;
    type?: string;
}

const SchemaInput = ({
    autoFocus,
    label,
    onChange,
    placeholder,
    type = 'text',
    value = 'Untitled',
}: SchemaInputProps) => {
    const [localValue, setLocalValue] = useState<string>(value);

    const onChangeValue = () => {
        if (localValue === value) {
            return;
        }

        onChange(localValue);
    };

    useEffect(() => {
        setLocalValue(value);
    }, [value]);

    return (
        <fieldset className="space-y-1 overflow-hidden p-0.5">
            <Label>{label}</Label>

            <Input
                autoFocus={autoFocus}
                className="text-ellipsis"
                onBlur={onChangeValue}
                onChange={(e) => setLocalValue(e.target.value)}
                onKeyPress={handleEnterPress(onChangeValue)}
                placeholder={placeholder}
                type={type}
                value={localValue}
            />
        </fieldset>
    );
};

export default SchemaInput;
