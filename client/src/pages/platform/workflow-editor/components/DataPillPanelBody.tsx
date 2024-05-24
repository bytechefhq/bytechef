import {
    ActionDefinitionModel,
    ComponentDefinitionBasicModel,
    type PropertyModel,
    TriggerDefinitionModel,
} from '@/middleware/platform/configuration';
import DataPillPanelBodyInputsItem from '@/pages/platform/workflow-editor/components/DataPillPanelBodyInputsItem';
import getFilteredProperties from '@/pages/platform/workflow-editor/utils/getFilteredProperties';
import {PropertyType} from '@/types/types';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyPropertiesItem from './DataPillPanelBodyPropertiesItem';

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
                <AccordionItem className="group" value="inputs">
                    <DataPillPanelBodyInputsItem />
                </AccordionItem>

                {componentOperations.map((componentOperation: ComponentOperationType, index: number) => {
                    const outputSchema: PropertyType | undefined = componentOperations[index]?.outputSchema;

                    const properties: Array<PropertyType> | undefined = outputSchema?.properties;

                    const filteredProperties = properties?.length
                        ? getFilteredProperties({
                              filterQuery: dataPillFilterQuery,
                              properties,
                          })
                        : [];

                    if (!componentOperation.componentDefinition) {
                        return <></>;
                    }

                    return (
                        <AccordionItem
                            className="group"
                            key={`accordion-item-${componentOperation.workflowNodeName}`}
                            value={componentOperation.workflowNodeName}
                        >
                            <DataPillPanelBodyPropertiesItem
                                componentAction={componentOperation}
                                filteredProperties={filteredProperties}
                                outputSchemaExists={!!outputSchema}
                                sampleOutput={componentOperations[index].sampleOutput}
                            />
                        </AccordionItem>
                    );
                })}
            </Accordion>
        </div>
    </div>
);

export default DataPillPanelBody;
