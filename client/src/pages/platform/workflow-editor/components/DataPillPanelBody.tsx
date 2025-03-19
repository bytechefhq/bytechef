import {ScrollArea} from '@/components/ui/scroll-area';
import DataPillPanelBodyInputsItem from '@/pages/platform/workflow-editor/components/DataPillPanelBodyInputsItem';
import {
    ActionDefinition,
    ComponentDefinitionBasic,
    type Property,
    TaskDispatcherDefinitionBasic,
    TriggerDefinition,
} from '@/shared/middleware/platform/configuration';
import {Accordion, AccordionItem} from '@radix-ui/react-accordion';

import DataPillPanelBodyPropertiesItem from './DataPillPanelBodyPropertiesItem';

interface BaseComponentOperationI {
    workflowNodeName: string;
    outputSchema: Property;
    sampleOutput: object;
}

interface ComponentActionOperationI extends BaseComponentOperationI {
    componentDefinition: ComponentDefinitionBasic;
    actionDefinition: ActionDefinition;
    taskDispatcherDefinition: never;
}

interface ComponentTriggerOperationI extends BaseComponentOperationI {
    componentDefinition: ComponentDefinitionBasic;
    taskDispatcherDefinition: never;
    triggerDefinition: TriggerDefinition;
}

interface TaskDispatcherOperationI extends BaseComponentOperationI {
    componentDefinition: never;
    taskDispatcherDefinition: TaskDispatcherDefinitionBasic;
}

export type ComponentOperationType = ComponentActionOperationI | ComponentTriggerOperationI | TaskDispatcherOperationI;

interface DataPillPanelBodyProps {
    componentOperations: Array<ComponentOperationType>;
    dataPillFilterQuery: string;
}

const DataPillPanelBody = ({componentOperations, dataPillFilterQuery}: DataPillPanelBodyProps) => (
    <ScrollArea>
        <Accordion className="h-full" collapsible type="single">
            <AccordionItem className="group" value="inputs">
                <DataPillPanelBodyInputsItem />
            </AccordionItem>

            {componentOperations.map((operation) => (
                <AccordionItem
                    className="group"
                    key={`accordion-item-${operation.workflowNodeName}`}
                    value={operation.workflowNodeName}
                >
                    <DataPillPanelBodyPropertiesItem
                        componentOperation={operation}
                        dataPillFilterQuery={dataPillFilterQuery}
                        sampleOutput={operation.sampleOutput}
                    />
                </AccordionItem>
            ))}
        </Accordion>
    </ScrollArea>
);

export default DataPillPanelBody;
