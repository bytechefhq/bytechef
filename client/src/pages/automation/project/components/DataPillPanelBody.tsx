import {
    ActionDefinitionModel,
    ComponentDefinitionBasicModel,
    type PropertyModel,
    TriggerDefinitionModel,
} from '@/middleware/platform/configuration';
import getFilteredProperties from '@/pages/automation/project/utils/getFilteredProperties';
import {PropertyType} from '@/types/types';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyItem from './DataPillPanelBodyItem';

export interface ComponentActionI extends ActionDefinitionModel {
    componentDefinition: ComponentDefinitionBasicModel;
    outputSchema: PropertyModel;
    sampleOutput: object;
    workflowNodeName: string;
}

export interface ComponentTriggerI extends TriggerDefinitionModel {
    componentDefinition: ComponentDefinitionBasicModel;
    outputSchema: PropertyModel;
    sampleOutput: object;
    workflowNodeName: string;
}

export type ComponentOperationType = ComponentActionI & ComponentTriggerI;

interface DataPillPanelBodyProps {
    componentOperations: Array<ComponentOperationType>;
    dataPillFilterQuery: string;
}

const DataPillPanelBody = ({componentOperations, dataPillFilterQuery}: DataPillPanelBodyProps) => (
    <div className="relative h-full overflow-y-auto">
        <div className="absolute left-0 top-0 w-full">
            <Accordion className="h-full" collapsible type="single">
                {componentOperations.map((componentOperation: ComponentOperationType, index: number) => {
                    const outputSchema: PropertyType | undefined = componentOperations[index]?.outputSchema;

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
                            key={`accordion-item-${componentOperation.workflowNodeName}`}
                            value={componentOperation.workflowNodeName}
                        >
                            {componentOperations[index] && (
                                <DataPillPanelBodyItem
                                    componentAction={componentOperation}
                                    filteredProperties={filteredProperties}
                                    outputSchemaExists={!!outputSchema}
                                    sampleOutput={componentOperations[index].sampleOutput}
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
