/* eslint-disable @typescript-eslint/no-explicit-any */
/// <reference types="vite-plugin-svgr/client" />

import {
import {ActionDefinitionModel} from 'middleware/definition-registry';

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

const SchemaProperties = ({properties}: {properties: any}) => {
    return (
        <ul className="h-full">
            {properties.map((property: any, index: number) => {
                return (
                    <li
                        className="flex flex-col"
                        key={`${property.name}-${index}`}
                    >
                        <div className="inline-flex rounded-md p-2 text-sm hover:bg-gray-100">
                            <span>
                                {
                                    TYPE_ICONS[
                                        property.type as keyof typeof TYPE_ICONS
                                    ]
                                }
                            </span>

                            <span className="pl-2">{property.name}</span>
                        </div>

                        {property.properties &&
                            !!property.properties.length && (
                                <div
                                    key={property.name}
                                    className="mb-4 ml-4 flex flex-col border-l-2 border-gray-300"
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
};

const OutputTab = ({
    actionDefinition,
}: {
    actionDefinition: ActionDefinitionModel;
}) => {
    return (
        <div className="max-h-full flex-[1_1_1px] overflow-auto">
            {actionDefinition.outputSchema?.map((schema: any, index) =>
                schema.properties ? (
                    <SchemaProperties
                        key={schema.name + '_' + index}
                        properties={schema.properties}
                    />
                ) : (
                    <div
                        className="inline-flex rounded-md p-2 text-sm hover:bg-gray-100"
                        key={schema.name}
                    >
                        <span>
                            {TYPE_ICONS[schema.type as keyof typeof TYPE_ICONS]}
                        </span>

                        <span className="pl-2">{schema.controlType}</span>
                    </div>
                )
            )}
        </div>
    );
};

export default OutputTab;
