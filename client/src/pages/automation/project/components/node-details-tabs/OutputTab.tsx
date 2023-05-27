/// <reference types="vite-plugin-svgr/client" />

import {PropertyModel} from 'middleware/core/definition-registry';
import {TYPE_ICONS} from 'shared/typeIcons';

import {PropertyType} from '../../../../../types/projectTypes';
import {useNodeDetailsDialogStore} from '../../stores/useNodeDetailsDialogStore';

const PropertyField = ({data, label}: {data: PropertyType; label: string}) => (
    <div className="inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
        <span title={data.type}>
            {TYPE_ICONS[data.type as keyof typeof TYPE_ICONS]}
        </span>

        <span className="pl-2">{label}</span>
    </div>
);

const SchemaProperties = ({properties}: {properties: PropertyType[]}) => (
    <ul className="ml-2 h-full">
        {properties.map((property: PropertyType, index: number) => (
            <li className="flex flex-col" key={`${property.name}_${index}`}>
                <PropertyField data={property} label={property.name!} />

                {property.properties && !!property.properties.length && (
                    <div
                        key={property.name}
                        className="ml-3 flex flex-col border-l border-gray-200 pl-1"
                    >
                        <SchemaProperties properties={property.properties} />
                    </div>
                )}
            </li>
        ))}
    </ul>
);

const OutputTab = ({outputSchema}: {outputSchema: PropertyModel[]}) => {
    const {currentNode} = useNodeDetailsDialogStore();

    return (
        <div className="max-h-full flex-[1_1_1px] p-4">
            {outputSchema.map((schema: PropertyType, index) => (
                <div key={`${schema.name}_${index}`}>
                    <div className="mb-1 flex items-center">
                        <span title={schema.type}>
                            {TYPE_ICONS[schema.type as keyof typeof TYPE_ICONS]}
                        </span>

                        <span className="ml-2 text-sm text-gray-800">
                            {currentNode.name}
                        </span>
                    </div>

                    {schema.properties && (
                        <SchemaProperties properties={schema.properties} />
                    )}

                    {!schema.properties && !!schema.controlType && (
                        <PropertyField
                            data={schema}
                            label={schema.controlType!}
                        />
                    )}
                </div>
            ))}
        </div>
    );
};

export default OutputTab;
