import {Label} from '@/components/ui/label';
import {Toggle} from '@/components/ui/toggle';
import React from 'react';

const handleChange = (handler: (value: boolean) => void) => {
    return (event: React.FormEvent<HTMLElement>): void => {
        handler((event.target as HTMLInputElement).checked);
    };
};

interface SchemaCheckboxProps {
    value: boolean;
    onChange: (v: boolean) => void;
    label?: string;
}

const SchemaCheckbox = ({label, onChange, value}: SchemaCheckboxProps) => {
    return (
        <div className="flex flex-row">
            <Label>{label}</Label>

            <Toggle className="ml-2" defaultChecked={value} onChange={handleChange(onChange)} />
        </div>
    );
};

export default SchemaCheckbox;
