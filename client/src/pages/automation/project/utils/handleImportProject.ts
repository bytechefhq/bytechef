import {ImportProjectRequest} from '@/shared/middleware/automation/configuration';
import {UseMutationResult} from '@tanstack/react-query';
import {ChangeEvent} from 'react';

const handleImportProject = (
    event: ChangeEvent<HTMLInputElement>,
    currentWorkspaceId: number,
    importProjectMutation: UseMutationResult<number, Error, ImportProjectRequest, unknown>
) => {
    const file = event.target.files?.[0];

    if (!file) {
        return;
    }

    importProjectMutation.mutate({
        file,
        workspaceId: currentWorkspaceId,
    });

    if (event.target) {
        event.target.value = '';
    }
};

export default handleImportProject;
