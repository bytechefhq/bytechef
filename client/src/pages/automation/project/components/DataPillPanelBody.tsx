import {
    ActionDefinitionModel,
    ComponentDefinitionBasicModel,
    type PropertyModel,
} from '@/middleware/platform/configuration';
import getFilteredProperties from '@/pages/automation/project/utils/getFilteredProperties';
import {PropertyType} from '@/types/types';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyItem from './DataPillPanelBodyItem';

export interface ComponentActionDataI extends ActionDefinitionModel {
    componentDefinition: ComponentDefinitionBasicModel;
    outputSchema: PropertyModel;
    sampleOutput: object;
    workflowNodeName: string;
}

interface DataPillPanelBodyProps {
    componentActionData: Array<ComponentActionDataI>;
    dataPillFilterQuery: string;
}

const DataPillPanelBody = ({componentActionData, dataPillFilterQuery}: DataPillPanelBodyProps) => (
    <div className="relative h-full overflow-y-auto">
        <div className="absolute left-0 top-0 w-full">
            <Accordion className="h-full" collapsible type="single">
                {componentActionData.map((componentAction: ComponentActionDataI, index: number) => {
                    const outputSchema: PropertyType | undefined = componentActionData[index]?.outputSchema;

                    const properties: Array<PropertyType> | undefined = outputSchema?.properties;

                    const filteredProperties = properties?.length
                        ? getFilteredProperties({
                              filterQuery: dataPillFilterQuery,
                              properties,
                          })
                        : [];

                    return (
                        <AccordionItem
                            className="group"
                            key={`accordion-item-${componentAction.workflowNodeName}`}
                            value={componentAction.workflowNodeName}
                        >
                            {componentActionData[index] && (
                                <DataPillPanelBodyItem
                                    componentAction={componentAction}
                                    filteredProperties={filteredProperties}
                                    sampleOutput={componentActionData[index].sampleOutput}
                                />
                            )}
                        </AccordionItem>
                    );
                })}
            </Accordion>
        </div>
    </div>
);

export default DataPillPanelBody;
