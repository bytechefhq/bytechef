/* eslint-disable sort-keys */

import {
    GetComponentTriggerDefinitionRequest,
    GetComponentTriggerDefinitionsRequest,
    TriggerDefinitionApi,
} from '@/shared/middleware/platform/configuration/apis/TriggerDefinitionApi';
import {TriggerDefinition} from '@/shared/middleware/platform/configuration/models/TriggerDefinition';
import {useQuery} from '@tanstack/react-query';

import type {TriggerDefinitionBasic} from '@/shared/middleware/platform/configuration';

export const TriggerDefinitionKeys = {
    triggerDefinition: (request: GetComponentTriggerDefinitionRequest) => [
        ...TriggerDefinitionKeys.triggerDefinitions,
        request.componentName,
        request.componentVersion,
        request.triggerName,
    ],
    filteredTriggerDefinitions: (request: GetComponentTriggerDefinitionsRequest) => [
        ...TriggerDefinitionKeys.triggerDefinitions,
        request.componentName,
        request.componentVersion,
    ],
    triggerDefinitions: ['triggerDefinitions'] as const,
};

export const useGetTriggerDefinitionQuery = (request: GetComponentTriggerDefinitionRequest, enabled?: boolean) =>
    useQuery<TriggerDefinition, Error>({
        queryKey: TriggerDefinitionKeys.triggerDefinition(request),
        queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetTriggerDefinitionsQuery = (request: GetComponentTriggerDefinitionsRequest, enabled?: boolean) =>
    useQuery<TriggerDefinitionBasic[], Error>({
        queryKey: TriggerDefinitionKeys.filteredTriggerDefinitions(request),
        queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinitions(request),
        enabled: enabled === undefined ? true : enabled,
    });
