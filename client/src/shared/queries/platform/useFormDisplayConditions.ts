import {ComponentOperationType, useComponentPropertyDisplayConditionsQuery} from '@/shared/middleware/graphql';
import {useEffect, useRef, useState} from 'react';

interface UseFormDisplayConditionsPropsI {
    componentName?: string;
    componentVersion?: number;
    enabled?: boolean;
    operationName?: string;
    operationType?: ComponentOperationType;
    parameters: Record<string, unknown>;
}

interface UseFormDisplayConditionsReturnI {
    displayConditions: Record<string, boolean> | undefined;
    isEvaluating: boolean;
}

const DEBOUNCE_MS = 300;

const useFormDisplayConditions = ({
    componentName,
    componentVersion,
    enabled = true,
    operationName,
    operationType,
    parameters,
}: UseFormDisplayConditionsPropsI): UseFormDisplayConditionsReturnI => {
    const [debouncedParameters, setDebouncedParameters] = useState(parameters);

    const lastDisplayConditionsRef = useRef<Record<string, boolean> | undefined>(undefined);

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

    const displayConditions = data?.componentPropertyDisplayConditions as Record<string, boolean> | undefined;

    if (displayConditions) {
        lastDisplayConditionsRef.current = displayConditions;
    }

    return {
        displayConditions: displayConditions ?? lastDisplayConditionsRef.current,
        isEvaluating: queryEnabled && lastDisplayConditionsRef.current === undefined,
    };
};

export default useFormDisplayConditions;
