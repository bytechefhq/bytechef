import {ProjectKeys} from '@/shared/queries/automation/projects.queries';
import {QueryClient} from '@tanstack/react-query';

const handleImportProject = async (
    event: React.ChangeEvent<HTMLInputElement>,
    currentWorkspaceId: number,
    queryClient: QueryClient
) => {
    const file = event.target.files?.[0];

    if (!file) {
        return;
    }

    const formData = new FormData();

    formData.append('file', file);

    try {
        const response = await fetch(`/api/automation/internal/workspaces/${currentWorkspaceId}/projects/import`, {
            body: formData,
            method: 'POST',
        });

        if (response.ok) {
            queryClient.invalidateQueries({
                queryKey: ProjectKeys.projects,
            });
        } else {
            console.error('Failed to import project');
        }
    } catch (error) {
        console.error('Error importing project:', error);
    }

    if (event.target) {
        event.target.value = '';
    }
};

export default handleImportProject;
