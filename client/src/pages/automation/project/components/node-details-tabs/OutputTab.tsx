import {ComponentOutputSchemaModel} from '@/middleware/platform/configuration';
import {PropertyType} from '@/types/projectTypes';

/// <reference types="vite-plugin-svgr/client" />

import {TYPE_ICONS} from 'shared/typeIcons';

import {useWorkflowNodeDetailsPanelStore} from '../../stores/useWorkflowNodeDetailsPanelStore';

const PropertyField = ({data, label = 'item'}: {data: PropertyType; label: string}) => (
    <div className="inline-flex items-center rounded-md p-1 text-sm hover:bg-gray-100">
        <span title={data.type}>{TYPE_ICONS[data.type as keyof typeof TYPE_ICONS]}</span>

        <span className="pl-2">{label}</span>
    </div>
);

const SchemaProperties = ({properties}: {properties: Array<PropertyType>}) => (
    <ul className="ml-2 h-full">
        {properties.map((property, index) => (
            <li className="flex flex-col" key={`${property.name}_${index}`}>
                <PropertyField data={property} label={property.name!} />

                {property.properties && !!property.properties.length && (
                    <div
                        className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                        key={property.name}
                    >
                        <SchemaProperties properties={property.properties} />
                    </div>
                )}

                {property.items && !!property.items.length && (
                    <div
                        className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1"
                        key={property.name}
                    >
                        <SchemaProperties properties={property.items} />
                    </div>
                )}
            </li>
        ))}
    </ul>
);

const OutputTab = ({outputSchema}: {outputSchema: ComponentOutputSchemaModel}) => {
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    return (
        <div className="max-h-full flex-[1_1_1px] p-4">
            <div className="mb-1 flex items-center">
                <span title={outputSchema.definition.type}>
                    {TYPE_ICONS[outputSchema.definition.type as keyof typeof TYPE_ICONS]}
                </span>

                <span className="ml-2 text-sm text-gray-800">{currentNode.name}</span>
            </div>

            {(outputSchema.definition as PropertyType)?.properties && (
                <SchemaProperties properties={(outputSchema.definition as PropertyType).properties!} />
            )}

            {!(outputSchema.definition as PropertyType).properties &&
                !!(outputSchema.definition as PropertyType).controlType && (
                    <PropertyField
                        data={outputSchema.definition}
                        label={(outputSchema.definition as PropertyType).controlType!}
                    />
                )}
        </div>
    );
};

export default OutputTab;
