import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {KeyboardEvent, useEffect, useRef, useState} from 'react';

const handleEnterPress = (handler: () => void) => {
    return (event: KeyboardEvent<HTMLInputElement>): void => {
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

    const localValueRef = useRef(localValue);

    localValueRef.current = localValue;

    const onChangeRef = useRef(onChange);

    onChangeRef.current = onChange;

    const valueRef = useRef(value);

    valueRef.current = value;

    const onChangeValue = () => {
        if (localValue === value) {
            return;
        }

        onChange(localValue);
    };

    useEffect(() => {
        setLocalValue(value);
    }, [value]);

    useEffect(() => {
        return () => {
            if (localValueRef.current !== valueRef.current) {
                onChangeRef.current(localValueRef.current);
            }
        };
    }, []);

    return (
        <fieldset className="space-y-1 overflow-hidden p-0.5">
            <Label>{label}</Label>

            <Input
                autoFocus={autoFocus}
                className="text-ellipsis"
                onBlur={onChangeValue}
                onChange={(event) => setLocalValue(event.target.value)}
                onKeyDown={handleEnterPress(onChangeValue)}
                placeholder={placeholder}
                type={type}
                value={localValue}
            />
        </fieldset>
    );
};

export default SchemaInput;
