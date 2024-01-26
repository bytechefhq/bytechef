import {
    ActionDefinitionModel,
    ComponentDefinitionBasicModel,
    type PropertyModel,
} from '@/middleware/platform/configuration';
import getFilteredProperties from '@/pages/automation/project/utils/getFilteredProperties';
import {PropertyType} from '@/types/projectTypes';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyItem from './DataPillPanelBodyItem';

export type ComponentActionData = {
    componentDefinition: ComponentDefinitionBasicModel;
    outputSchema: PropertyModel;
    sampleOutput: object;
    workflowNodeName: string;
} & ActionDefinitionModel;

type DataPillPanelBodyProps = {
    componentActionData: Array<ComponentActionData>;
    dataPillFilterQuery: string;
};

const DataPillPanelBody = ({componentActionData, dataPillFilterQuery}: DataPillPanelBodyProps) => (
    <div className="relative h-full overflow-y-auto">
        <div className="absolute left-0 top-0 w-full">
            <Accordion className="h-full" collapsible type="single">
                {componentActionData.map((componentAction: ComponentActionData, index: number) => {
                    const outputSchemaDefinition: PropertyType | undefined = componentActionData[index]?.outputSchema;

                    const properties: Array<PropertyType> | undefined = outputSchemaDefinition?.properties?.length
                        ? outputSchemaDefinition.properties
                        : outputSchemaDefinition?.items;

                    let existingProperties = properties?.filter((property) => {
                        if (property.name) {
                            return true;
                        } else {
                            if (property.properties || property.items) {
                                return true;
                            }
                        }
                    });

                    if (outputSchemaDefinition && outputSchemaDefinition.type === 'FILE_ENTRY') {
                        existingProperties = [
                            {
                                controlType: outputSchemaDefinition.controlType,
                                description: outputSchemaDefinition.description,
                                label: outputSchemaDefinition.label,
                                name: outputSchemaDefinition.name || componentAction.workflowNodeName || 'fileEntry',
                                required: outputSchemaDefinition.required,
                                type: outputSchemaDefinition.type,
                            },
                        ];
                    }

                    const filteredProperties = existingProperties?.length
                        ? getFilteredProperties({
                              filterQuery: dataPillFilterQuery,
                              properties: existingProperties,
                          })
                        : [];

                    return (
                        <AccordionItem
                            className="group"
                            key={`accordion-item-${componentAction.workflowNodeName}`}
                            value={componentAction.workflowNodeName}
                        >
                            <DataPillPanelBodyItem
                                componentAction={componentAction}
                                filteredProperties={filteredProperties}
                            />
                        </AccordionItem>
                    );
                })}
            </Accordion>
        </div>
    </div>
);

export default DataPillPanelBody;
