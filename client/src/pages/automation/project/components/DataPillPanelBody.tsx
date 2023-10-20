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
import {useEffect} from 'react';
import InlineSVG from 'react-inlinesvg';

import {useNodeDetailsDialogStore} from '../stores/useNodeDetailsDialogStore';
import useWorkflowDefinitionStore from '../stores/useWorkflowDefinitionStore';
import DataPill from './DataPill';

type dataPillType = {display: string; id: string};

const DataPillPanelBody = ({
    containerHeight,
    dataPillFilterQuery,
}: {
    containerHeight: number;
    dataPillFilterQuery: string;
}) => {
    const {componentActions, componentNames, setDataPills} =
        useWorkflowDefinitionStore();

    const {currentNode} = useNodeDetailsDialogStore();

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

    const getSubProperties = (
        properties: PropertyType[],
        propertyName: string,
        componentTitle: string
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
    ): any =>
        properties.map((subProperty: PropertyType) => {
            if (subProperty.properties?.length) {
                return getSubProperties(
                    subProperty.properties,
                    propertyName,
                    componentTitle
                );
            } else if (subProperty.items?.length) {
                return getSubProperties(
                    subProperty.items,
                    propertyName,
                    componentTitle
                );
            }

            return {
                display: `${componentTitle}/${propertyName}/${
                    subProperty.label || subProperty.name
                }`,
                id: `${componentTitle}/${propertyName}/${subProperty.name}`,
            };
        });

    const componentProperties = previousComponents?.map((component, index) => {
        if (!actionData?.length) {
            return;
        }

        const outputSchema: PropertyType | undefined =
            actionData[index]?.outputSchema;

        const properties = outputSchema?.properties?.length
            ? outputSchema.properties
            : outputSchema?.items;

        return {
            componentName: component.title,
            properties,
        };
    });

    const getExistingProperties = (
        properties: PropertyType[]
    ): PropertyType[] =>
        properties.filter((property) => {
            if (property.properties) {
                return getExistingProperties(property.properties);
            } else if (property.items) {
                return getExistingProperties(property.items);
            }

            return !!property.name;
        });

    const availableDataPills: dataPillType[] = [];

    componentProperties?.forEach((componentProperty) => {
        if (!componentProperty) {
            return;
        }

        const existingProperties = getExistingProperties(
            componentProperty.properties!
        );

        const formattedProperties: dataPillType[] = existingProperties.map(
            (property: PropertyType) => {
                if (property.properties) {
                    return getSubProperties(
                        property.properties,
                        property.name!,
                        componentProperty.componentName!
                    );
                } else if (property.items) {
                    return getSubProperties(
                        property.items,
                        property.name!,
                        componentProperty.componentName!
                    );
                }

                return {
                    display: `${componentProperty.componentName}/${
                        property.label || property.name
                    }`,
                    id: property.name,
                };
            }
        );

        if (existingProperties.length && formattedProperties.length) {
            availableDataPills.push(...formattedProperties);
        }
    });

    useEffect(() => {
        if (availableDataPills) {
            setDataPills(availableDataPills.flat(Infinity));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [availableDataPills.length]);

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
                                                        onClick={() =>
                                                            console.log('TODO')
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
