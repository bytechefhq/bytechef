import {ActionDefinitionModel, ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import getFilteredProperties from '@/pages/automation/project/utils/getFilteredProperties';
import {PropertyType} from '@/types/projectTypes';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyItem from './DataPillPanelBodyItem';

export type ComponentActionData = {
    componentDefinition: ComponentDefinitionBasicModel;
    workflowAlias: string;
} & ActionDefinitionModel;

type DataPillPanelBodyProps = {
    componentData: Array<ComponentActionData>;
    dataPillFilterQuery: string;
};

const DataPillPanelBody = ({componentData, dataPillFilterQuery}: DataPillPanelBodyProps) => (
    <div className="relative h-full overflow-y-auto">
        <div className="absolute left-0 top-0 w-full">
            <Accordion className="h-full" collapsible type="single">
                {componentData.map((componentAction: ComponentActionData, index: number) => {
                    const outputSchema: PropertyType | undefined = componentData[index]?.outputSchema;

                    const properties = outputSchema?.properties?.length ? outputSchema.properties : outputSchema?.items;

                    const existingProperties = properties?.filter((property) => !!property.name);

                    const filteredProperties = existingProperties?.length
                        ? getFilteredProperties({
                              filterQuery: dataPillFilterQuery,
                              properties: existingProperties,
                          })
                        : [];

                    return (
                        <AccordionItem
                            className="group"
                            key={`accordion-item-${componentAction.workflowAlias}`}
                            value={componentAction.workflowAlias}
                        >
                            {!!filteredProperties?.length && (
                                <DataPillPanelBodyItem
                                    componentAction={componentAction}
                                    filteredProperties={filteredProperties}
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
