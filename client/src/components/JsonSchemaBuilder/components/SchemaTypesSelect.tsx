import {Label} from '@/components/ui/label';

import {SCHEMA_TYPES} from '../utils/constants';
import * as helpers from '../utils/helpers';
import {SchemaType} from '../utils/types';

import '../../CreatableSelect/CreatableSelect.css';

import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {TYPE_ICONS} from '@/shared/typeIcons';

interface SchemaTypesSelectProps {
    type: SchemaType;
    onChange: (type: SchemaType) => void;
}

const SchemaTypesSelect = ({onChange, type}: SchemaTypesSelectProps) => (
    <fieldset className="space-y-1 p-0.5">
        <Label>Pill Type</Label>

        <Select
            defaultValue="object"
            onValueChange={(option: SchemaType) => onChange(option)}
            value={helpers.findOption(type)(SCHEMA_TYPES)?.value}
        >
            <SelectTrigger className="flex w-full min-w-48">
                <SelectValue className="flex" placeholder="Type" />
            </SelectTrigger>

            <SelectContent>
                {SCHEMA_TYPES.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                        <div className="flex w-full items-center space-x-2">
                            <span>{TYPE_ICONS[option.value.toUpperCase() as keyof typeof TYPE_ICONS]}</span>

                            <span>{option.label}</span>
                        </div>
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    </fieldset>
);

export default SchemaTypesSelect;
