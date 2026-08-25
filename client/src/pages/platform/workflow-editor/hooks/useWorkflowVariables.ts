import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {VariableI, VariableScopeType, getVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

export default function useWorkflowVariables(): VariableI[] | undefined {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentType = usePlatformTypeStore((state) => state.currentType);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const scope = useMemo<VariableScopeType | undefined>(() => {
        if (currentType === PlatformType.EMBEDDED) {
            return {type: 'EMBEDDED'};
        }

        return currentWorkspaceId != null ? {type: 'WORKSPACE', workspaceId: currentWorkspaceId} : undefined;
    }, [currentType, currentWorkspaceId]);

    const {data} = getVariablesApi().useWorkflowVariablesQuery(scope, currentEnvironmentId);

    return data;
}
