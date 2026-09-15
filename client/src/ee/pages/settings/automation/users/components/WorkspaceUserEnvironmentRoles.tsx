import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {
    ENVIRONMENT_LABELS,
    ENVIRONMENT_ORDER,
    type WorkspaceUserEnvironmentRoleI,
} from '@/ee/pages/settings/automation/users/util/workspace-environment-roles';
import {CUSTOM_ROLE_PREFIX} from '@/ee/pages/settings/automation/users/util/workspace-role-values';
import {EnvironmentEnum, WorkspaceRole} from '@/shared/middleware/graphql';
import {getRoleLabel} from '@/shared/util/role-utils';
import {Trash2Icon} from 'lucide-react';
import {useMemo, useState} from 'react';

const WORKSPACE_ROLES = Object.values(WorkspaceRole);

interface WorkspaceUserEnvironmentRolesProps {
    customRoles: {id: string; name: string}[];
    environmentRoles: WorkspaceUserEnvironmentRoleI[];
    manageableEnvironments: EnvironmentEnum[];
    onRemove: (environment: EnvironmentEnum) => void;
    onRoleChange: (environment: EnvironmentEnum, value: string) => void;
}

const WorkspaceUserEnvironmentRoles = ({
    customRoles,
    environmentRoles,
    manageableEnvironments,
    onRemove,
    onRoleChange,
}: WorkspaceUserEnvironmentRolesProps) => {
    const [pendingRemoval, setPendingRemoval] = useState<EnvironmentEnum | null>(null);

    const orderedRoles = useMemo(
        () =>
            ENVIRONMENT_ORDER.map((environment) =>
                environmentRoles.find((environmentRole) => environmentRole.environment === environment)
            ).filter((environmentRole): environmentRole is WorkspaceUserEnvironmentRoleI => environmentRole != null),
        [environmentRoles]
    );

    const missingEnvironments = useMemo(
        () =>
            ENVIRONMENT_ORDER.filter(
                (environment) =>
                    manageableEnvironments.includes(environment) &&
                    !environmentRoles.some((environmentRole) => environmentRole.environment === environment)
            ),
        [environmentRoles, manageableEnvironments]
    );

    const isLastEnvironmentRole = orderedRoles.length === 1;

    const canManageEveryEnvironment = manageableEnvironments.length === ENVIRONMENT_ORDER.length;

    const pendingRemovalRoleLabel = useMemo(() => {
        const pendingRemovalRole = orderedRoles.find(
            (environmentRole) => environmentRole.environment === pendingRemoval
        );

        if (!pendingRemovalRole) {
            return '';
        }

        if (pendingRemovalRole.customRoleId) {
            return (
                customRoles.find((customRole) => customRole.id === String(pendingRemovalRole.customRoleId))?.name || ''
            );
        }

        return pendingRemovalRole.workspaceRole ? getRoleLabel(pendingRemovalRole.workspaceRole) : '';
    }, [customRoles, orderedRoles, pendingRemoval]);

    return (
        <div className="space-y-2">
            {orderedRoles.map((environmentRole) => (
                <div className="flex items-center gap-2" key={environmentRole.environment}>
                    <span className="w-32 shrink-0 text-sm text-muted-foreground">
                        {ENVIRONMENT_LABELS[environmentRole.environment]}
                    </span>

                    <Select
                        disabled={!manageableEnvironments.includes(environmentRole.environment)}
                        onValueChange={(value) => onRoleChange(environmentRole.environment, value)}
                        value={
                            environmentRole.customRoleId
                                ? `${CUSTOM_ROLE_PREFIX}${environmentRole.customRoleId}`
                                : (environmentRole.workspaceRole ?? undefined)
                        }
                    >
                        <SelectTrigger
                            aria-label={`${ENVIRONMENT_LABELS[environmentRole.environment]} role`}
                            className="w-44"
                        >
                            <SelectValue placeholder="Select a role" />
                        </SelectTrigger>

                        <SelectContent>
                            {WORKSPACE_ROLES.map((workspaceRole) => (
                                <SelectItem key={workspaceRole} value={workspaceRole}>
                                    {getRoleLabel(workspaceRole)}
                                </SelectItem>
                            ))}

                            {customRoles.map((customRole) => (
                                <SelectItem key={customRole.id} value={`${CUSTOM_ROLE_PREFIX}${customRole.id}`}>
                                    {customRole.name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    {manageableEnvironments.includes(environmentRole.environment) &&
                        (canManageEveryEnvironment || !isLastEnvironmentRole) && (
                            <button
                                aria-label={`Remove ${ENVIRONMENT_LABELS[environmentRole.environment]} role`}
                                className="text-destructive hover:text-destructive/80"
                                onClick={() => setPendingRemoval(environmentRole.environment)}
                                type="button"
                            >
                                <Trash2Icon className="size-4" />
                            </button>
                        )}
                </div>
            ))}

            {missingEnvironments.length > 0 && (
                <div className="flex items-center gap-2">
                    <span aria-hidden className="w-32 shrink-0" />

                    <Select
                        onValueChange={(environment) =>
                            onRoleChange(environment as EnvironmentEnum, WorkspaceRole.Viewer)
                        }
                        value=""
                    >
                        <SelectTrigger aria-label="Grant a role in another environment" className="w-44">
                            <SelectValue placeholder="Add environment..." />
                        </SelectTrigger>

                        <SelectContent>
                            {missingEnvironments.map((environment) => (
                                <SelectItem key={environment} value={environment}>
                                    {ENVIRONMENT_LABELS[environment]}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>
            )}

            {pendingRemoval && (
                <AlertDialog open={true}>
                    <AlertDialogContent>
                        <AlertDialogHeader>
                            <AlertDialogTitle>Remove the {ENVIRONMENT_LABELS[pendingRemoval]} role?</AlertDialogTitle>

                            <AlertDialogDescription>
                                {isLastEnvironmentRole
                                    ? `This is their only environment role. They will keep the ${pendingRemovalRoleLabel} role in every environment.`
                                    : `They will lose access to ${ENVIRONMENT_LABELS[pendingRemoval]}. Their roles in the other environments are unchanged.`}
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel onClick={() => setPendingRemoval(null)}>Cancel</AlertDialogCancel>

                            <AlertDialogAction
                                className="bg-red-600"
                                onClick={() => {
                                    onRemove(pendingRemoval);
                                    setPendingRemoval(null);
                                }}
                            >
                                Remove
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            )}
        </div>
    );
};

export default WorkspaceUserEnvironmentRoles;
