import {PropertyType} from '@/shared/types';

import PropertyField from './PropertyField';

interface SchemaPropertiesProps {
    copiedValue: string | null;
    copyToClipboard: (text: string) => Promise<void>;
    parentPath?: string;
    properties: Array<PropertyType>;
    sampleOutput: object;
    workflowNodeName: string;
}

const SchemaProperties = ({
    copiedValue,
    copyToClipboard,
    parentPath,
    properties,
    sampleOutput,
    workflowNodeName,
}: SchemaPropertiesProps) => (
    <ul className="ml-2 h-full">
        {properties.map((property, index) => {
            const {items, name, properties} = property;

            const path = `${parentPath ? parentPath + (name ? '.' : '') : ''}${name || '[index]'}`;

            return (
                <li className="flex flex-col" key={`${name}_${index}`}>
                    <PropertyField
                        copiedValue={copiedValue}
                        copyToClipboard={copyToClipboard}
                        label={name!}
                        parentPath={parentPath}
                        property={property}
                        sampleOutput={sampleOutput}
                        workflowNodeName={workflowNodeName}
                    />

                    {properties && !!properties.length && (
                        <div className="ml-3 flex flex-col overflow-y-auto border-l border-gray-200 pl-1" key={name}>
                            <SchemaProperties
                                copiedValue={copiedValue}
                                copyToClipboard={copyToClipboard}
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
                                copiedValue={copiedValue}
                                copyToClipboard={copyToClipboard}
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
