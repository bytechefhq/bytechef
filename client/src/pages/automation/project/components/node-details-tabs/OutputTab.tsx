/// <reference types="vite-plugin-svgr/client" />

import {PropertyModel} from 'middleware/definition-registry';

import {ReactComponent as ArrayIcon} from '../../../../../assets/array.svg';
import {ReactComponent as BooleanIcon} from '../../../../../assets/boolean.svg';
import {ReactComponent as DateIcon} from '../../../../../assets/date.svg';
import {ReactComponent as DateTimeIcon} from '../../../../../assets/datetime.svg';
import {ReactComponent as DynamicIcon} from '../../../../../assets/dynamic.svg';
import {ReactComponent as IntegerIcon} from '../../../../../assets/integer.svg';
import {ReactComponent as NullIcon} from '../../../../../assets/null.svg';
import {ReactComponent as NumberIcon} from '../../../../../assets/number.svg';
import {ReactComponent as ObjectIcon} from '../../../../../assets/object.svg';
import {ReactComponent as OneOfIcon} from '../../../../../assets/oneof.svg';
import {ReactComponent as StringIcon} from '../../../../../assets/string.svg';
import {ReactComponent as TimeIcon} from '../../../../../assets/time.svg';
import {PropertyType} from '../../../../../types/projectTypes';
import {useNodeDetailsDialogStore} from '../../stores/useNodeDetailsDialogStore';

const TYPE_ICONS = {
    ARRAY: <ArrayIcon className="h-5 w-5 text-gray-600" />,
    BOOLEAN: <BooleanIcon className="h-5 w-5 text-gray-600" />,
    DATE: <DateIcon className="h-5 w-5 text-gray-600" />,
    DATE_TIME: <DateTimeIcon className="h-5 w-5 text-gray-600" />,
    DYNAMIC_PROPERTIES: <DynamicIcon className="h-5 w-5 text-gray-600" />,
    INTEGER: <IntegerIcon className="h-5 w-5 text-gray-600" />,
    NUMBER: <NumberIcon className="h-5 w-5 text-gray-600" />,
    NULL: <NullIcon className="h-5 w-5 text-gray-600" />,
    OBJECT: <ObjectIcon className="h-5 w-5 text-gray-600" />,
    ONE_OF: <OneOfIcon className="h-5 w-5 text-gray-600" />,
    STRING: <StringIcon className="h-5 w-5 text-gray-600" />,
    TIME: <TimeIcon className="h-5 w-5 text-gray-600" />,
};

const PropertyField = ({data, label}: {data: PropertyType; label: string}) => (
    <div className="inline-flex rounded-md p-2 text-sm hover:bg-gray-100">
        <span>{TYPE_ICONS[data.type as keyof typeof TYPE_ICONS]}</span>

        <span className="pl-2">{label}</span>
    </div>
);

const SchemaProperties = ({properties}: {properties: PropertyType[]}) => (
    <ul className="ml-2 h-full">
        {properties.map((property: PropertyType, index: number) => {
            return (
                <li className="flex flex-col" key={`${property.name}_${index}`}>
                    <PropertyField data={property} label={property.name!} />

                    {property.properties && !!property.properties.length && (
                        <div
                            key={property.name}
                            className="ml-4 flex flex-col border-l border-gray-200 pl-1"
                        >
                            <SchemaProperties
                                properties={property.properties}
                            />
                        </div>
                    )}
                </li>
            );
        })}
    </ul>
);

const OutputTab = ({outputSchema}: {outputSchema: PropertyModel[]}) => {
    const {currentNode} = useNodeDetailsDialogStore();

    return (
        <div className="max-h-full flex-[1_1_1px] overflow-auto">
            <div className="mb-2 flex items-center">
                <span>{TYPE_ICONS.OBJECT}</span>

                <span className="ml-2 text-sm text-gray-800">
                    {currentNode.name}
                </span>
            </div>

            {outputSchema.map((schema: PropertyType, index) =>
                schema.properties ? (
                    <SchemaProperties
                        key={`${schema.name}_${index}`}
                        properties={schema.properties}
                    />
                ) : (
                    <PropertyField
                        data={schema}
                        key={`${schema.name}_${index}`}
                        label={schema.controlType!}
                    />
                )
            )}
        </div>
    );
};

export default OutputTab;
