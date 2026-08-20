import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import ResourceVisibilityPicker, {
    ResourceVisibilityValueType,
} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {useProjectVisibility} from '@/shared/hooks/useProjectVisibility';

interface ProjectVisibilityDialogProps {
    onClose: () => void;
    projectId: number;
    visibility?: ResourceVisibilityValueType;
}

const ProjectVisibilityDialog = ({onClose, projectId, visibility}: ProjectVisibilityDialogProps) => {
    const projectVisibility = useProjectVisibility({projectId, visibility});

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Project Visibility</DialogTitle>

                        <DialogDescription>
                            Decide who in the workspace can see this project, its workflows, deployments and executions.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                {projectVisibility.enabled && (
                    <ResourceVisibilityPicker
                        grantedUserIds={projectVisibility.grantedUserIds}
                        onGrantedUserIdsChange={projectVisibility.onGrantedUserIdsChange}
                        onVisibilityChange={projectVisibility.onVisibilityChange}
                        visibility={visibility || 'WORKSPACE'}
                        workspaceMembers={projectVisibility.workspaceMembers}
                    />
                )}
            </DialogContent>
        </Dialog>
    );
};

export default ProjectVisibilityDialog;
