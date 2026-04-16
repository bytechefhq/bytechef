import {NodeDataType, PropertyAllType} from '@/shared/types';
import {useEffect, useMemo} from 'react';
import {useForm} from 'react-hook-form';

interface UseClusterElementTestPropertiesPopoverProps {
    currentNode: NodeDataType;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onSubmit: (inputParameters: Record<string, any>) => void;
    properties: PropertyAllType[];
}

function isExpression(value: unknown): boolean {
    return typeof value === 'string' && (value.startsWith('=') || value.includes('${'));
}

function filterValue(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.filter((item) => !isExpression(item)).map((item) => filterValue(item));
    }

    if (value && typeof value === 'object') {
        return filterExpressions(value as Record<string, unknown>);
    }

    return value;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function filterExpressions(obj: Record<string, any>): Record<string, any> {
    return Object.fromEntries(
        Object.entries(obj)
            .filter(([, value]) => !isExpression(value))
            .map(([key, value]) => [key, filterValue(value)])
    );
}

export default function useClusterElementTestPropertiesPopover({
    currentNode,
    onSubmit,
    properties,
}: UseClusterElementTestPropertiesPopoverProps) {
    const filteredDefaultValues = useMemo(() => {
        const parameters = currentNode.parameters ?? {};

        return filterExpressions(parameters);
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
