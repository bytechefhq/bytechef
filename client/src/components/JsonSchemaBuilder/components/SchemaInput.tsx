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

    const onChangeValue = () => {
        if (localVal === value) {
            return;
        }

        onChange(localVal);
    };

    return (
        <div className="w-full">
            <Label>{label}</Label>

            <Input
                onBlur={onChangeValue}
                onChange={(e) => setLocalVal(e.target.value)}
                onKeyPress={handleEnterPress(onChangeValue)}
                placeholder={placeholder}
                type={type}
                value={localVal}
            />
        </div>
    );
};

export default SchemaInput;
