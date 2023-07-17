import {ComponentDefinitionModel} from '@/middleware/helios/execution/models';
import {useGetActionDefinitionsQuery} from '@/queries/actionDefinitions.queries';
import {useGetComponentDefinitionsQuery} from '@/queries/componentDefinitions.queries';
import {PropertyType} from '@/types/projectTypes';
import {
    Accordion,
    AccordionContent,
    AccordionItem,
    AccordionTrigger,
} from '@radix-ui/react-accordion';
import {ChevronDownIcon} from 'lucide-react';
import {MouseEvent} from 'react';
import InlineSVG from 'react-inlinesvg';

import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import DataPill from './DataPill';

const DataPillPanelBody = ({
    containerHeight,
    dataPillFilterQuery,
}: {
    containerHeight: number;
    dataPillFilterQuery: string;
}) => {
    const {componentActions, componentNames, dataPills, setDataPills} =
        useWorkflowDefinitionStore();

    const {currentNode, focusedInput} = useNodeDetailsDialogStore();

    const taskTypes = componentActions?.map(
        (componentAction) =>
            `${componentAction.componentName}/1/${componentAction.actionName}`
    );

    const currentNodeIndex = componentNames.indexOf(currentNode.name);

    const previousComponentNames =
        componentNames.length > 1
            ? componentNames.slice(0, currentNodeIndex)
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

    const getMatchingSubProperty = (
        properties: PropertyType[],
        name: string
    ): PropertyType | undefined => {
        const matchingProperties = properties.map((subProperty) => {
            if (subProperty.label === name || subProperty.name === name) {
                return subProperty;
            } else if (subProperty.properties) {
                return getMatchingSubProperty(subProperty.properties, name);
            } else if (subProperty.items) {
                return getMatchingSubProperty(subProperty.items, name);
            }
        });

        if (matchingProperties) {
            return matchingProperties.filter(
                (property) => property !== undefined
            )[0];
        } else {
            return undefined;
        }
    };

    const handleDataPillClick = (
        event: MouseEvent<HTMLDivElement>,
        property: PropertyType
    ) => {
        let dataPillData = property.label || property.name;

        if (!(event.target instanceof HTMLDivElement)) {
            return;
        }

        const eventData = event.currentTarget.dataset.name;

        if (!eventData) {
            return;
        }

        const subProperties = property.properties || property.items;

        if (property.name !== eventData && subProperties?.length) {
            const matchingProperty = getMatchingSubProperty(
                subProperties,
                eventData
            );

            if (matchingProperty) {
                dataPillData = matchingProperty.label || matchingProperty.name;
            }
        }

        if (focusedInput && dataPillData) {
            const existingDataPill = dataPills.find(
                (pill) => pill.name === focusedInput.name
            );

            if (existingDataPill) {
                const remainingDataPills = dataPills.filter(
                    (pill) => pill.name !== focusedInput.name
                );

                setDataPills([
                    ...remainingDataPills,
                    {
                        name: focusedInput.name,
                        value: [...existingDataPill.value, dataPillData],
                    },
                ]);
            } else {
                setDataPills([
                    ...dataPills,
                    {
                        name: focusedInput.name,
                        value: [dataPillData],
                    },
                ]);
            }
        }
    };

    const getFilteredProperties = (
        properties: PropertyType[],
        filterQuery: string
    ) =>
        properties?.reduce((previousValue: PropertyType[], currentValue) => {
            const subProperties = getFilteredProperties(
                currentValue.properties || currentValue.items || [],
                filterQuery
            );

            if (
                currentValue.name
                    ?.toLowerCase()
                    .includes(filterQuery.toLowerCase()) ||
                subProperties.length
            ) {
                previousValue.push(Object.assign({}, currentValue));
            }

            return previousValue;
        }, []);

    return (
        <Accordion className="h-full" collapsible type="single">
            {previousComponents?.map(
                (component: ComponentDefinitionModel, index: number) => {
                    const {icon, name, title} = component;

                    if (!actionData?.length) {
                        return;
                    }

                    const outputSchema: PropertyType | undefined =
                        actionData[index]?.outputSchema;

                    const properties = outputSchema?.properties?.length
                        ? outputSchema.properties
                        : outputSchema?.items;

                    const existingProperties = properties?.filter(
                        (property) => !!property.name
                    );

                    const filteredProperties = existingProperties?.length
                        ? getFilteredProperties(
                              existingProperties,
                              dataPillFilterQuery
                          )
                        : [];

                    return (
                        <AccordionItem
                            className="group group-data-[state=open]:h-full"
                            key={name}
                            style={{
                                maxHeight: containerHeight / 2,
                            }}
                            value={name}
                        >
                            {!!filteredProperties?.length && (
                                <>
                                    <AccordionTrigger className="group flex w-full items-center justify-between border-gray-100 bg-white p-4 group-data-[state=closed]:border-b">
                                        <div className="flex items-center space-x-4">
                                            {icon && (
                                                <div className="flex h-5 w-5 items-center">
                                                    <InlineSVG src={icon} />
                                                </div>
                                            )}

                                            <span className="text-sm">
                                                {title}
                                            </span>
                                        </div>

                                        <ChevronDownIcon className="h-5 w-5 text-gray-400 transition-transform duration-300 group-data-[state=open]:rotate-180" />
                                    </AccordionTrigger>

                                    <AccordionContent
                                        className="w-full space-y-4 overflow-y-scroll border-b border-gray-100 bg-gray-100 p-2 group-data-[state=open]:h-full"
                                        style={{
                                            maxHeight: containerHeight / 2 - 52,
                                        }}
                                    >
                                        <ul className="flex w-full flex-col space-y-2 group-data-[state=open]:h-full">
                                            {filteredProperties?.map(
                                                (property: PropertyType) => (
                                                    <DataPill
                                                        key={property.name}
                                                        onClick={(
                                                            event: MouseEvent<HTMLDivElement>
                                                        ) =>
                                                            handleDataPillClick(
                                                                event,
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
                }
            )}
        </Accordion>
    );
};

export default DataPillPanelBody;
