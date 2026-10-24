import Button from '@/components/Button/Button';
import DataSyncDialog from '@/pages/automation/data-syncs/components/DataSyncDialog';
import {Project} from '@/shared/middleware/automation/configuration';
import {useState} from 'react';

interface ProjectDataSyncCreationActionsProps {
    placement: 'emptyState' | 'tabRow';
    project: Project;
}

const ProjectDataSyncCreationActions = ({placement, project}: ProjectDataSyncCreationActionsProps) => {
    const [showDataSyncDialog, setShowDataSyncDialog] = useState(false);

    const compact = placement === 'tabRow';

    return (
        <>
            <Button
                aria-label={compact ? 'New Data Sync' : 'Create Data Sync'}
                className={compact ? undefined : 'mx-auto'}
                onClick={(event) => {
                    event.stopPropagation();

                    setShowDataSyncDialog(true);
                }}
                size={compact ? 'sm' : undefined}
                variant={compact ? 'outline' : 'default'}
            >
                {compact ? 'New Data Sync' : 'Create Data Sync'}
            </Button>

            {showDataSyncDialog && (
                <DataSyncDialog
                    onOpenChange={setShowDataSyncDialog}
                    open={showDataSyncDialog}
                    projectId={project.id!}
                />
            )}
        </>
    );
};

export default ProjectDataSyncCreationActions;
export type {ProjectDataSyncCreationActionsProps};
