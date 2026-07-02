import {useCallback, useEffect, useRef, useState} from 'react';
import {ApiFetch, OptionType} from './types';
import {optionsCacheKey} from './utils';

type LoadOptionsArgs = [
    componentName: string,
    componentVersion: number,
    groupName: string,
    propertyName: string,
    lookupDependsOnValues: Record<string, unknown>,
];

interface UseWorkflowInputOptionsReturnType {
    loadOptions: (
        componentName: string,
        componentVersion: number,
        groupName: string,
        propertyName: string,
        lookupDependsOnValues: Record<string, unknown>
    ) => void;
    optionsByKey: Record<string, OptionType[]>;
    resetOptions: () => void;
}

const EMPTY_OPTIONS: Record<string, OptionType[]> = {};

export default function useWorkflowInputOptions(
    apiFetch: ApiFetch | undefined,
    integrationInstanceId: number | undefined
): UseWorkflowInputOptionsReturnType {
    // Options are stored per integration instance. Switching integrations therefore returns a different (empty) slice
    // by construction, so the previous integration's options can never leak into the new one — no reset, no effect,
    // and no race with the just-mounted select field's first fetch.
    const [optionsByInstance, setOptionsByInstance] = useState<Record<number, Record<string, OptionType[]>>>({});

    const optionsByInstanceRef = useRef<Record<number, Record<string, OptionType[]>>>({});
    const inFlightKeysRef = useRef<Set<string>>(new Set());
    const generationRef = useRef(0);
    // Requests issued before apiFetch / integrationInstanceId are ready, keyed by cache key so duplicates collapse.
    const pendingRequestsRef = useRef<Map<string, LoadOptionsArgs>>(new Map());

    const loadOptions = useCallback(
        (
            componentName: string,
            componentVersion: number,
            groupName: string,
            propertyName: string,
            lookupDependsOnValues: Record<string, unknown>
        ) => {
            const cacheKey = optionsCacheKey(
                componentName,
                componentVersion,
                groupName,
                propertyName,
                lookupDependsOnValues
            );

            // A dynamic select can mount before the integration instance id resolves on first open. Queue the
            // request so it fires once the id is available instead of leaving the field stuck on "No options".
            if (!apiFetch || !integrationInstanceId) {
                pendingRequestsRef.current.set(cacheKey, [
                    componentName,
                    componentVersion,
                    groupName,
                    propertyName,
                    lookupDependsOnValues,
                ]);

                return;
            }

            const instanceCache = optionsByInstanceRef.current[integrationInstanceId] ?? {};
            const inFlightKey = `${integrationInstanceId}:${cacheKey}`;

            if (instanceCache[cacheKey] !== undefined || inFlightKeysRef.current.has(inFlightKey)) {
                return;
            }

            inFlightKeysRef.current.add(inFlightKey);

            const requestGeneration = generationRef.current;

            void apiFetch<OptionType[]>(
                `/api/embedded/v1/integration-instances/${integrationInstanceId}/component-input-options`,
                {
                    body: {componentName, componentVersion, groupName, lookupDependsOnValues, propertyName},
                    method: 'POST',
                }
            )
                .then((options) => {
                    if (generationRef.current !== requestGeneration) {
                        return;
                    }

                    const previousInstanceCache = optionsByInstanceRef.current[integrationInstanceId] ?? {};

                    optionsByInstanceRef.current = {
                        ...optionsByInstanceRef.current,
                        [integrationInstanceId]: {...previousInstanceCache, [cacheKey]: options ?? []},
                    };

                    setOptionsByInstance(optionsByInstanceRef.current);
                })
                .catch((error: unknown) => {
                    console.error('Failed to load workflow input options:', (error as Error).message);
                })
                .finally(() => {
                    inFlightKeysRef.current.delete(inFlightKey);
                });
        },
        [apiFetch, integrationInstanceId]
    );

    const resetOptions = useCallback(() => {
        generationRef.current += 1;

        optionsByInstanceRef.current = {};

        inFlightKeysRef.current.clear();
        pendingRequestsRef.current.clear();

        setOptionsByInstance({});
    }, []);

    // Flush any requests that were queued while apiFetch / integrationInstanceId were not yet ready.
    useEffect(() => {
        if (!apiFetch || !integrationInstanceId || pendingRequestsRef.current.size === 0) {
            return;
        }

        const pendingRequests = Array.from(pendingRequestsRef.current.values());

        pendingRequestsRef.current.clear();

        for (const args of pendingRequests) {
            loadOptions(...args);
        }
    }, [apiFetch, integrationInstanceId, loadOptions]);

    return {
        loadOptions,
        optionsByKey: integrationInstanceId != null ? (optionsByInstance[integrationInstanceId] ?? EMPTY_OPTIONS) : EMPTY_OPTIONS,
        resetOptions,
    };
}
