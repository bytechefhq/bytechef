import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {VariableI, VariableScopeType, getVariablesApi} from '@/shared/edition/variables/variablesApi';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useMemo} from 'react';

/**
 * Variables visible to the workflow open in the editor: the current workspace's set in automation, the
 * organization's set in embedded, always for the editor's current environment.
 *
 * Returns exactly what the edition seam returns — `undefined` or `VariableI[]` — WITHOUT collapsing `undefined`
 * to `[]`. That distinction is load-bearing (see `variablesApi.ts`): `undefined` means this build has no
 * variables feature at all (the CE default), while `[]` means the feature exists but nothing is defined yet.
 * Consumers that only need pills to append (where both cases behave identically — no extra pills) may safely
 * coalesce the result with `?? []`. Consumers that decide whether to render a "Variables" section at all — see
 * `DataPillPanelBody` — must NOT coalesce, or CE would render an empty section pointing at a settings page that
 * edition does not have.
 */
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
