import React from 'react';

import {SchemaFieldOptionValueType, SchemaMenuOptionType, SchemaRecordType} from '../utils/types';
import {
    BoolItem,
    CreatableMultiSelectItem,
    NumberItem,
    RequiredMultiSelectItem,
    SelectItem,
    TextItem,
} from './SchemaMenuItem';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const typeToItem: Record<SchemaFieldOptionValueType, React.FunctionComponent<any>> = {
    boolean: (props) => <BoolItem {...props} />,
    multi_creatable: (props) => <CreatableMultiSelectItem {...props} />,
    number: (props) => <NumberItem {...props} />,
    required: (props) => <RequiredMultiSelectItem {...props} />,
    select: (props) => <SelectItem {...props} />,
    text: (props) => <TextItem {...props} />,
};

interface SchemaMenuListProps {
    fields: SchemaMenuOptionType[];
    onChange: (schema: SchemaRecordType) => void;
    schema: SchemaRecordType;
}

const SchemaMenuList = ({fields, onChange, schema}: SchemaMenuListProps) => {
    return (
        <ul className="mb-4 grid gap-2">
            {fields.map((field) => (
                <li className="" key={field.value}>
                    {typeToItem[field.type]({field, onChange, schema})}
                </li>
            ))}
        </ul>
    );
};

export default SchemaMenuList;
