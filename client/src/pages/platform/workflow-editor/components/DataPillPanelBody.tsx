import DataPillPanelBodyInputsItem from '@/pages/platform/workflow-editor/components/DataPillPanelBodyInputsItem';
import {
    ActionDefinition,
    ComponentDefinitionBasic,
    type Property,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyPropertiesItem from './DataPillPanelBodyPropertiesItem';

export interface ComponentActionI extends ActionDefinition {
    componentDefinition: ComponentDefinitionBasic;
    outputSchema: Property;
    sampleOutput: object;
    workflowNodeName: string;
}

export interface ComponentTriggerI extends TriggerDefinition {
    componentDefinition: ComponentDefinitionBasic;
    outputSchema: Property;
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
                                componentOperation={componentOperation}
                                dataPillFilterQuery={dataPillFilterQuery}
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
