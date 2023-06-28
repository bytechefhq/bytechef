import {ChevronDownIcon} from '@heroicons/react/24/outline';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import * as Dialog from '@radix-ui/react-dialog';
import {Cross1Icon} from '@radix-ui/react-icons';
import Button from 'components/Button/Button';
import {useGetActionDefinitionsQuery} from 'queries/actionDefinitions.queries';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import InlineSVG from 'react-inlinesvg';
import {PropertyType} from 'types/projectTypes';

import {useDataPillPanelStore} from '../stores/useDataPillPanelStore';
import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import DataPill from './DataPill';

const DataPillPanelBody = () => {
    const {componentActions, componentNames} = useWorkflowDefinitionStore();
    const {focusedInput} = useNodeDetailsDialogStore();

    const taskTypes = componentActions?.map(
        (componentAction) =>
            `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const previousComponentNames =
        componentNames.length > 1
            ? componentNames.slice(0, -1)
            : componentNames;

    const {data: previousComponents} = useGetComponentDefinitionsQuery(
        {
            include: previousComponentNames,
        },
        !!componentNames.length
    );

    const {data: actionData} = useGetActionDefinitionsQuery(
        {taskTypes},
        !!componentActions?.length
    );

    const handleDataPillClick = (property: PropertyType) => {
        const dataPillData = property.label || property.name;

        if (focusedInput && dataPillData) {
            focusedInput.value = dataPillData;
        }
    };

    return (
        <Accordion type="multiple">
            {previousComponents?.map((component, index) => {
                const {icon, name, title} = component;

                if (!actionData?.length) {
                    return;
                }

                const outputSchema: PropertyType | undefined =
                    actionData[index].outputSchema;

                const properties = outputSchema?.properties?.length
                    ? outputSchema.properties
                    : outputSchema?.items;

                const existingProperties = properties?.filter(
                    (datum) => !!datum.name
                );

                return (
                    <AccordionItem className="group" key={name} value={name}>
                        {!!existingProperties?.length && (
                            <>
                                <AccordionTrigger className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b">
                                    <div className="flex items-center space-x-4">
                                        {icon && (
                                            <div className="flex h-5 w-5 items-center">
                                                <InlineSVG src={icon} />
                                            </div>
                                        )}

                                        <span className="text-sm">{title}</span>
                                    </div>

                                    <ChevronDownIcon className="h-5 w-5 text-gray-400 transition-transform duration-300 ease-[cubic-bezier(0.87,_0,_0.13,_1)] group-data-[state=open]:rotate-180" />
                                </AccordionTrigger>

                                <AccordionContent className="w-full space-y-4 border-b border-gray-100 bg-gray-100 p-2 group-data-[state=open]:h-full">
                                    <ul className="flex w-full flex-col space-y-2">
                                        {existingProperties?.map(
                                            (property: PropertyType) => (
                                                <DataPill
                                                    key={property.name}
                                                    onClick={() =>
                                                        handleDataPillClick(
                                                            property
                                                        )
                                                    }
                                                    property={property}
                                                />
                                            )
                                        )}
                                    </ul>
                                </AccordionContent>
                            </>
                        )}
                    </AccordionItem>
                );
            })}
        </Accordion>
    );
};

const DataPillPanel = () => {
    const {dataPillPanelOpen, setDataPillPanelOpen} = useDataPillPanelStore();
    const {nodeDetailsDialogOpen} = useNodeDetailsDialogStore();

    return (
        <Dialog.Root
            open={dataPillPanelOpen && nodeDetailsDialogOpen}
            onOpenChange={() => setDataPillPanelOpen(!dataPillPanelOpen)}
            modal={false}
        >
            <Dialog.Portal>
                <Dialog.Content
                    className="fixed inset-y-2 right-[492px] top-16 z-10 w-screen max-w-[320px] overflow-hidden rounded-xl border-l bg-white shadow-lg"
                    onInteractOutside={(event) => event.preventDefault()}
                >
                    <div className="flex h-full flex-col bg-white shadow-xl">
                        <header className="border-b border-gray-100 p-4">
                            <Dialog.Title className="flex content-center items-center text-lg font-medium text-gray-900">
                                <span>Data Pill Panel</span>

                                <Button
                                    aria-label="Close the data pill panel"
                                    className="ml-auto"
                                    displayType="icon"
                                    icon={
                                        <Cross1Icon
                                            className="h-3 w-3 cursor-pointer text-gray-900"
                                            aria-hidden="true"
                                        />
                                    }
                                    onClick={() => setDataPillPanelOpen(false)}
                                />
                            </Dialog.Title>

                            <Dialog.Description className="text-sm">
                                To use data from the previous step drag the
                                datapill into a field, or click on the datapill.
                            </Dialog.Description>
                        </header>

                        <main className="flex flex-col">
                            <DataPillPanelBody />
                        </main>
                    </div>
                </Dialog.Content>
            </Dialog.Portal>
        </Dialog.Root>
    );
};

export default DataPillPanel;
