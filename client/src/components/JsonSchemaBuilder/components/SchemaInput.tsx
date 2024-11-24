import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import React, {FormEvent, useEffect, useState} from 'react';
import {useDebouncedCallback} from 'use-debounce';

const handleStringChange = (handler: (value: string) => void) => {
    return (event: FormEvent<HTMLElement>): void => {
        handler((event.target as HTMLInputElement).value);
    };
};

const handleEnterPress = (handler: () => void) => {
    return (event: React.KeyboardEvent<HTMLInputElement>): void => {
        if (event.key === 'Enter') {
            handler();
        }
    };
};

interface SchemaInputProps {
    onChange: (text: string) => void;
    placeholder?: string;
    value?: string;
    label?: string;
    type?: string;
}

const SchemaInput = ({label, onChange, placeholder, type = 'text', value = ''}: SchemaInputProps) => {
    const [localVal, setLocalVal] = useState<string>(value);

    useEffect(() => {
        setLocalVal(value);
    }, [value]);

    const handleChangeValue = useDebouncedCallback(() => {
        onChange(localVal);
    }, 1000);

    return (
        <div className="w-full">
            <Label>{label}</Label>

            <Input
                onBlur={() => handleChangeValue()}
                onChange={handleStringChange((value) => {
                    setLocalVal(value);
                    handleChangeValue();
                })}
                onKeyPress={handleEnterPress(() => onChange(localVal))}
                placeholder={placeholder}
                type={type}
                value={localVal}
            />
        </div>
    );
};

export default SchemaInput;
