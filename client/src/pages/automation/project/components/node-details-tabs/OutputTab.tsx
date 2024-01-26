import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuLabel, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {PropertyModel} from '@/middleware/platform/configuration';
import {PropertyType} from '@/types/projectTypes';
import {ChevronDownIcon} from 'lucide-react';

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

const OutputTab = ({outputSchema}: {outputSchema: PropertyModel}) => {
    const {currentNode} = useWorkflowNodeDetailsPanelStore();

    return (
        <div className="h-full p-4">
            {outputSchema ? (
                <>
                    <div className="mb-1 flex items-center">
                        <span title={outputSchema.type}>
                            {TYPE_ICONS[outputSchema.type as keyof typeof TYPE_ICONS]}
                        </span>

                        <span className="ml-2 text-sm text-gray-800">{currentNode.name}</span>
                    </div>

                    {(outputSchema as PropertyType)?.properties && (
                        <SchemaProperties properties={(outputSchema as PropertyType).properties!} />
                    )}

                    {!(outputSchema as PropertyType).properties && !!(outputSchema as PropertyType).controlType && (
                        <PropertyField data={outputSchema} label={(outputSchema as PropertyType).controlType!} />
                    )}
                </>
            ) : (
                <div className="flex size-full items-center justify-center">
                    <div className="flex flex-col items-center gap-4">
                        <div>Generate Schema and Sample Data</div>

                        <div>
                            <div className="inline-flex rounded-md shadow-sm">
                                <Button
                                    className="relative inline-flex items-center rounded-l-md rounded-r-none px-3 py-2 text-sm focus:z-10"
                                    type="button"
                                >
                                    Test component
                                </Button>

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild className="relative -ml-px block">
                                        <Button className="relative inline-flex items-center rounded-l-none rounded-r-md p-2 focus:z-10">
                                            <ChevronDownIcon aria-hidden="true" className="size-5" />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end" className="w-56 cursor-pointer">
                                        <DropdownMenuLabel>Upload sample data</DropdownMenuLabel>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default OutputTab;
