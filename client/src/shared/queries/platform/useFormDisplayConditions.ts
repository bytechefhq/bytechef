import {ComponentOperationType, useComponentPropertyDisplayConditionsQuery} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';

interface UseFormDisplayConditionsPropsI {
    componentName?: string;
    componentVersion?: number;
    enabled?: boolean;
    operationName?: string;
    operationType?: ComponentOperationType;
    parameters: Record<string, unknown>;
}

const DEBOUNCE_MS = 300;

const useFormDisplayConditions = ({
    componentName,
    componentVersion,
    enabled = true,
    operationName,
    operationType,
    parameters,
}: UseFormDisplayConditionsPropsI): Record<string, boolean> | undefined => {
    const [debouncedParameters, setDebouncedParameters] = useState(parameters);

    const serializedParameters = JSON.stringify(parameters);

    const queryEnabled =
        enabled &&
        !!componentName &&
        componentVersion !== undefined &&
        !!operationName &&
        !!operationType &&
        Object.keys(parameters).length > 0;

    const {data} = useComponentPropertyDisplayConditionsQuery(
        {
            componentName: componentName ?? '',
            componentVersion: componentVersion ?? 1,
            operationName: operationName ?? '',
            operationType: operationType ?? ComponentOperationType.Action,
            parameters: debouncedParameters,
        },
        {enabled: queryEnabled}
    );

    useEffect(() => {
        const timeout = setTimeout(() => setDebouncedParameters(JSON.parse(serializedParameters)), DEBOUNCE_MS);

        return () => clearTimeout(timeout);
    }, [serializedParameters]);

    return data?.componentPropertyDisplayConditions as Record<string, boolean> | undefined;
};

export default useFormDisplayConditions;
