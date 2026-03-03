import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

const FROM_AI_PATTERN = /^=fromAi\(/;

interface UseClusterElementTestPropertiesPopoverProps {
    currentNode: NodeDataType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onSubmit: (inputParameters: Record<string, any>) => void;
    properties: PropertyAllType[];
}

export default function useClusterElementTestPropertiesPopover({
    currentNode,
    onSubmit,
    properties,
}: UseClusterElementTestPropertiesPopoverProps) {
    const filteredDefaultValues = useMemo(() => {
        const parameters = currentNode.parameters ?? {};

        return Object.fromEntries(
            Object.entries(parameters).filter(
                ([, value]) => !(typeof value === 'string' && FROM_AI_PATTERN.test(value))
            )
        );
    }, [currentNode.parameters]);

    const propertiesWithDefaults = useMemo(
        () =>
            properties.map((property) => {
                const parameterValue = property.name ? filteredDefaultValues[property.name] : undefined;

                if (parameterValue !== undefined) {
                    return {
                        ...property,
                        defaultValue: parameterValue,
                    } as unknown as PropertyAllType;
                }

                return property;
            }),
        [filteredDefaultValues, properties]
    );

    const form = useForm({
        defaultValues: filteredDefaultValues,
    });

    useEffect(() => {
        form.reset(filteredDefaultValues);
    }, [filteredDefaultValues, form]);

    const {control, formState, handleSubmit} = form;

    const propertyTypeMap = useMemo(() => {
        const typeMap: Record<string, string> = {};

        for (const property of properties) {
            if (property.name && property.type) {
                typeMap[property.name] = property.type;
            }
        }

        return typeMap;
    }, [properties]);

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function handleFormSubmit(values: Record<string, any>) {
        const sanitizedValues = Object.fromEntries(
            Object.entries(values).map(([key, value]) => {
                if (value === '') {
                    return [key, null];
                }

                const propertyType = propertyTypeMap[key];

                if (
                    (propertyType === 'INTEGER' || propertyType === 'NUMBER') &&
                    typeof value === 'string' &&
                    value !== ''
                ) {
                    const numericValue = propertyType === 'INTEGER' ? parseInt(value, 10) : parseFloat(value);

                    return [key, isNaN(numericValue) ? value : numericValue];
                }

                return [key, value];
            })
        );

        onSubmit(sanitizedValues);
    }

    return {control, form, formState, handleFormSubmit, handleSubmit, propertiesWithDefaults};
}
