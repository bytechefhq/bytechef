import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain, DialogTrigger} from '@/components/Dialog';
import ResourceVisibilityBadge from '@/shared/components/visibility/ResourceVisibilityBadge';
import ResourceVisibilityPicker, {
    ResourceVisibilityValueType,
} from '@/shared/components/visibility/ResourceVisibilityPicker';
import {
    AppInstance,
    ResourceVisibility,
    useAppInstanceGrantsQuery,
    useGrantAppInstanceAccessMutation,
    useRevokeAppInstanceAccessMutation,
    useSetAppInstanceVisibilityMutation,
    useWorkspaceUsersQuery,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useMemo, useState} from 'react';

interface AppInstanceShareDialogProps {
    appInstance: Pick<AppInstance, 'id' | 'name' | 'visibility' | 'workspaceId'>;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

const AppInstanceShareDialog = ({appInstance, onOpenChange, open, triggerNode}: AppInstanceShareDialogProps) => {
    const [isOpen, setIsOpen] = useState(false);

    const queryClient = useQueryClient();

    const dialogOpen = open ?? isOpen;

    const visibility = (appInstance.visibility || 'WORKSPACE') as ResourceVisibilityValueType;

    const identifiers = {appInstanceId: appInstance.id, workspaceId: appInstance.workspaceId};

    const {data: appInstanceGrantsData} = useAppInstanceGrantsQuery(identifiers, {
        enabled: dialogOpen && visibility === 'PRIVATE',
    });

    const {data: workspaceUsersData} = useWorkspaceUsersQuery(
        {workspaceId: appInstance.workspaceId},
        {enabled: dialogOpen && visibility === 'PRIVATE'}
    );

    const grantAppInstanceAccessMutation = useGrantAppInstanceAccessMutation({
        onSuccess: invalidateAppInstanceSharing,
    });

    const revokeAppInstanceAccessMutation = useRevokeAppInstanceAccessMutation({
        onSuccess: invalidateAppInstanceSharing,
    });

    const setAppInstanceVisibilityMutation = useSetAppInstanceVisibilityMutation({
        onSuccess: invalidateAppInstanceSharing,
    });

    const grantedUserIds = useMemo(
        () => (appInstanceGrantsData?.appInstanceGrants ?? []).map(Number),
        [appInstanceGrantsData]
    );

    const workspaceMembers = useMemo(
        () =>
            (workspaceUsersData?.workspaceUsers ?? []).map((workspaceUser) => ({
                label: workspaceUser.user?.email ?? `User ${workspaceUser.userId}`,
                userId: Number(workspaceUser.userId),
            })),
        [workspaceUsersData]
    );

    function invalidateAppInstanceSharing() {
        queryClient.invalidateQueries({queryKey: ['app']});
        queryClient.invalidateQueries({queryKey: ['appInstance']});
        queryClient.invalidateQueries({queryKey: ['appInstanceGrants']});
        queryClient.invalidateQueries({queryKey: ['apps']});
        queryClient.invalidateQueries({queryKey: ['hostedAppInstances']});
    }

    const handleGrantedUserIdsChange = (nextUserIds: number[]) => {
        grantedUserIds
            .filter((userId) => !nextUserIds.includes(userId))
            .forEach((userId) => revokeAppInstanceAccessMutation.mutate({...identifiers, userId: String(userId)}));

        nextUserIds
            .filter((userId) => !grantedUserIds.includes(userId))
            .forEach((userId) => grantAppInstanceAccessMutation.mutate({...identifiers, userId: String(userId)}));
    };

    const handleVisibilityChange = (nextVisibility: ResourceVisibilityValueType) =>
        setAppInstanceVisibilityMutation.mutate({...identifiers, visibility: nextVisibility as ResourceVisibility});

    const handleOpenChange = (nextOpen: boolean) => {
        setIsOpen(nextOpen);

        onOpenChange?.(nextOpen);
    };

    return (
        <Dialog onOpenChange={handleOpenChange} open={dialogOpen}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogMain>
                    <DialogHeader
                        description="Decide who in the instance's workspace can use this instance."
                        title={`Share ${appInstance.name}`}
                    />

                    <DialogBody>
                        <div className="flex items-center gap-2 text-sm">
                            <span className="text-content-neutral-secondary">Currently</span>

                            <ResourceVisibilityBadge grantedUserCount={grantedUserIds.length} visibility={visibility} />
                        </div>

                        <ResourceVisibilityPicker
                            grantedUserIds={grantedUserIds}
                            onGrantedUserIdsChange={handleGrantedUserIdsChange}
                            onVisibilityChange={handleVisibilityChange}
                            visibility={visibility}
                            workspaceMembers={workspaceMembers}
                        />
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppInstanceShareDialog;
