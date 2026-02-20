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
        const result: Record<string, unknown> = {};

        for (const [key, value] of Object.entries(parameters)) {
            if (typeof value === 'string' && FROM_AI_PATTERN.test(value)) {
                continue;
            }

            result[key] = value;
        }

        return result;
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
        defaultValues: {
            parameters: filteredDefaultValues,
        },
    });

    useEffect(() => {
        form.reset({parameters: filteredDefaultValues});
    }, [filteredDefaultValues, form]);

    const {control, formState, handleSubmit} = form;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function handleFormSubmit(values: {parameters: Record<string, any>}) {
        onSubmit(values.parameters);
    }

    return {control, form, formState, handleFormSubmit, handleSubmit, propertiesWithDefaults};
}
