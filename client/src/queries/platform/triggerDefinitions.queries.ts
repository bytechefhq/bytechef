/* eslint-disable sort-keys */

import {
    GetComponentTriggerDefinitionRequest,
    TriggerDefinitionApi,
} from '@/middleware/platform/configuration/apis/TriggerDefinitionApi';
import {TriggerDefinitionModel} from '@/middleware/platform/configuration/models/TriggerDefinitionModel';
import {useQuery} from '@tanstack/react-query';

export const TriggerDefinitionKeys = {
    triggerDefinition: (request: GetComponentTriggerDefinitionRequest) => ['triggerDefinition', request],
};

export const useGetTriggerDefinitionQuery = (request: GetComponentTriggerDefinitionRequest, enabled?: boolean) =>
    useQuery<TriggerDefinitionModel, Error>({
        queryKey: TriggerDefinitionKeys.triggerDefinition(request),
        queryFn: () => new TriggerDefinitionApi().getComponentTriggerDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });
