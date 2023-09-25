import {PropertyType} from '@/types/projectTypes';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import getFilteredProperties from '../utils/getFilteredProperties';
import DataPill from './DataPill';
import {ComponentActionData} from './DataPillPanel';

type DataPillPanelBodyProps = {
    componentData: Array<ComponentActionData>;
    containerHeight: number;
    dataPillFilterQuery: string;
};

const DataPillPanelBody = ({
    componentData,
    containerHeight,
    dataPillFilterQuery,
}: DataPillPanelBodyProps) => (
    <Accordion className="h-full" collapsible type="single">
        {componentData.map(
            (componentAction: ComponentActionData, index: number) => {
                const {icon, title} = componentAction.component;

                const outputSchema: PropertyType | undefined =
                    componentData[index]?.outputSchema;

                const properties = outputSchema?.properties?.length
                    ? outputSchema.properties
                    : outputSchema?.items;

                const existingProperties = properties?.filter(
                    (property) => !!property.name
                );

                const filteredProperties = existingProperties?.length
                    ? getFilteredProperties({
                          filterQuery: dataPillFilterQuery,
                          properties: existingProperties,
                      })
                    : [];

                return (
                    <AccordionItem
                        className="group group-data-[state=open]:h-full"
                        key={`accordion-item-${componentAction.workflowAlias}`}
                        style={{
                            maxHeight: containerHeight / 2,
                        }}
                        value={componentAction.workflowAlias}
                    >
                        {!!filteredProperties?.length && (
                            <>
                                <AccordionTrigger
                                    key={`accordion-trigger-${componentAction.workflowAlias}`}
                                    className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b"
                                >
                                    <div className="flex items-center space-x-4">
                                        {icon && (
                                            <div className="flex h-5 w-5 items-center">
                                                <InlineSVG src={icon} />
                                            </div>
                                        )}

                                        <span className="text-sm">
                                            {title}

                                            <span className="pl-1 text-xs text-gray-400">
                                                ({componentAction.workflowAlias}
                                                )
                                            </span>
                                        </span>
                                    </div>

                                    <ChevronDownIcon className="h-5 w-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
                                </AccordionTrigger>

                                <AccordionContent
                                    className="w-full space-y-4 overflow-y-scroll border-b border-gray-100 bg-gray-100 p-2 group-data-[state=open]:h-full"
                                    style={{
                                        maxHeight: containerHeight / 2 - 52,
                                    }}
                                    key={`accordion-content-${componentAction.workflowAlias}`}
                                >
                                    <ul className="flex w-full flex-col space-y-2 group-data-[state=open]:h-full">
                                        {filteredProperties?.map((property) => (
                                            <DataPill
                                                component={
                                                    componentAction.component
                                                }
                                                componentAlias={
                                                    componentAction.workflowAlias
                                                }
                                                key={property.name}
                                                property={property}
                                            />
                                        ))}
                                    </ul>
                                </AccordionContent>
                            </>
                        )}
                    </AccordionItem>
                );
            }
        )}
    </Accordion>
);

export default DataPillPanelBody;
