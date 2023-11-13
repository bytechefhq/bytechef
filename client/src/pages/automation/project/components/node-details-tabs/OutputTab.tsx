/// <reference types="vite-plugin-svgr/client" />

import {TYPE_ICONS} from 'shared/typeIcons';

import {PropertyType} from '../../../../../types/projectTypes';
import {useWorkflowNodeDetailsPanelStore} from '../../stores/useWorkflowNodeDetailsPanelStore';

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
        {properties.map((property, index) => (
            <li className="flex flex-col" key={`${property.name}_${index}`}>
                <PropertyField data={property} label={property.name!} />

                {property.properties && !!property.properties.length && (
                    <div
                        className="ml-3 flex flex-col overflow-y-scroll border-l border-gray-200 pl-1"
                        key={property.name}
                    >
                        <SchemaProperties properties={property.properties} />
                    </div>
                )}
            </li>
        ))}
    </ul>
);

const OutputTab = ({outputSchema}: {outputSchema: PropertyType}) => {
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    return (
        <div className="max-h-full flex-[1_1_1px] p-4">
            <div className="mb-1 flex items-center">
                <span title={outputSchema.type}>
                    {TYPE_ICONS[outputSchema.type as keyof typeof TYPE_ICONS]}
                </span>

                <span className="ml-2 text-sm text-gray-800">
                    {currentNode.name}
                </span>
            </div>

            {outputSchema.properties && (
                <SchemaProperties properties={outputSchema.properties} />
            )}

            {!outputSchema.properties && !!outputSchema.controlType && (
                <PropertyField
                    data={outputSchema}
                    label={outputSchema.controlType!}
                />
            )}
        </div>
    );
};

export default OutputTab;
