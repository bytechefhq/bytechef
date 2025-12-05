import {ScrollArea} from '@/components/ui/scroll-area';
import DataPillPanelBodyInputsItem from '@/pages/platform/workflow-editor/components/datapills/DataPillPanelBodyInputsItem';
import {
    ActionDefinition,
    ComponentDefinitionBasic,
    type Property,
    TaskDispatcherDefinitionBasic,
    TriggerDefinition,
    WorkflowInput,
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

export type OperationType = ComponentActionOperationI | ComponentTriggerOperationI | TaskDispatcherOperationI;

interface DataPillPanelBodyProps {
    operations: Array<OperationType>;
    dataPillFilterQuery: string;
    workflowInputs?: WorkflowInput[];
}

const DataPillPanelBody = ({dataPillFilterQuery, operations, workflowInputs}: DataPillPanelBodyProps) => (
    <ScrollArea className="h-full w-full">
        <Accordion className="size-full max-w-data-pill-panel-width" collapsible type="single">
            {!!workflowInputs?.length && (
                <AccordionItem className="group" value="inputs">
                    <DataPillPanelBodyInputsItem />
                </AccordionItem>
            )}

            {operations.map((operation) => (
                <AccordionItem
                    className="group"
                    key={`accordion-item-${operation.workflowNodeName}`}
                    value={operation.workflowNodeName}
                >
                    <DataPillPanelBodyPropertiesItem
                        dataPillFilterQuery={dataPillFilterQuery}
                        operation={operation}
                        sampleOutput={operation.sampleOutput}
                    />
                </AccordionItem>
            ))}
        </Accordion>
    </ScrollArea>
);

export default DataPillPanelBody;
