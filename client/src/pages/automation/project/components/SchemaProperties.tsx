import {PropertyType} from '@/types/types';

import PropertyField from './PropertyField';

const SchemaProperties = ({
    parentPath,
    properties,
    sampleOutput,
    workflowNodeName,
}: {
    parentPath?: string;
    properties: Array<PropertyType>;
    sampleOutput: object;
    workflowNodeName: string;
}) => (
    <ul className="ml-2 h-full">
        {properties.map((property, index) => {
            const {items, name, properties} = property;

            const path = `${parentPath ? parentPath + (name ? '.' : '') : ''}${name || '[index]'}`;

            return (
                <li className="flex flex-col" key={`${name}_${index}`}>
                    <PropertyField
                        label={name!}
                        parentPath={parentPath}
                        property={property}
                        sampleOutput={sampleOutput}
                        workflowNodeName={workflowNodeName}
                    />

                    {properties && !!properties.length && (
                        <div className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1" key={name}>
                            <SchemaProperties
                                parentPath={path}
                                properties={properties}
                                sampleOutput={sampleOutput}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    )}

                    {items && !!items.length && (
                        <div className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1" key={name}>
                            <SchemaProperties
                                parentPath={path}
                                properties={items}
                                sampleOutput={sampleOutput}
                                workflowNodeName={workflowNodeName}
                            />
                        </div>
                    )}
                </li>
            );
        })}
    </ul>
);

export default SchemaProperties;
