import {EnvironmentEnum} from '@/shared/middleware/graphql';

/**
 * Deployment order rather than the enum's declaration order, so a member's rows read the way a promotion runs.
 */
export const ENVIRONMENT_ORDER: EnvironmentEnum[] = [
    EnvironmentEnum.Development,
    EnvironmentEnum.Staging,
    EnvironmentEnum.Production,
];

export const ENVIRONMENT_LABELS: Record<EnvironmentEnum, string> = {
    [EnvironmentEnum.Development]: 'Development',
    [EnvironmentEnum.Production]: 'Production',
    [EnvironmentEnum.Staging]: 'Staging',
};

export interface WorkspaceUserEnvironmentRoleI {
    customRoleId?: string | null;
    environment: EnvironmentEnum;
    workspaceRole?: string | null;
}

interface WorkspaceUserRowI {
    customRoleId?: string | null;
    environment?: string | null;
    id?: string | null;
    inherited?: boolean | null;
    user?: {email?: string | null; firstName?: string | null; lastName?: string | null} | null;
    userId: string;
    workspaceRole?: string | null;
}

export interface WorkspaceMemberI {
    environmentRoles: WorkspaceUserEnvironmentRoleI[];
    implicitRow: WorkspaceUserRowI | undefined;
    inherited: boolean;
    user: WorkspaceUserRowI['user'];
    userId: string;
}

/**
 * Collapses the membership rows the server returns into one entry per member.
 *
 * A member is either implicit -- a single row applying to every environment -- or explicit -- one row per environment,
 * where an absent row denies that environment. Rendering the raw rows would list the same person once per environment
 * with nothing saying why.
 */
export const toWorkspaceMembers = (workspaceUserRows: WorkspaceUserRowI[]): WorkspaceMemberI[] => {
    const membersByUserId = new Map<string, WorkspaceMemberI>();

    for (const workspaceUserRow of workspaceUserRows) {
        const userId = String(workspaceUserRow.userId);

        let member = membersByUserId.get(userId);

        if (!member) {
            member = {
                environmentRoles: [],
                implicitRow: undefined,
                inherited: Boolean(workspaceUserRow.inherited),
                user: workspaceUserRow.user,
                userId,
            };

            membersByUserId.set(userId, member);
        }

        if (workspaceUserRow.environment) {
            member.environmentRoles.push({
                customRoleId: workspaceUserRow.customRoleId,
                environment: workspaceUserRow.environment as EnvironmentEnum,
                workspaceRole: workspaceUserRow.workspaceRole,
            });
        } else {
            member.implicitRow = workspaceUserRow;
        }
    }

    return Array.from(membersByUserId.values());
};
