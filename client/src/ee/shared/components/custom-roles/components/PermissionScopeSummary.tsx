import {getRoleLabel, getScopeActionLabel} from '@/shared/util/role-utils';
import {useMemo} from 'react';

interface PermissionScopeGroupI {
    name: string;
    scopes: string[];
}

interface PermissionScopeSummaryProps {
    permissionScopeGroups: PermissionScopeGroupI[];
    scopes: string[];
}

/**
 * The held scopes of one role, grouped under their module headings — `CONNECTION` over `View, Edit`.
 *
 * Shared by the built-in tiers and the custom-role list rather than written twice, so a tier and a custom role always
 * describe a permission the same way. They did not before: the custom-role list rendered a flat sentence of full scope
 * labels, so the same grant read as `Connection View, Connection Delete, Workflow View` there and as two headed groups
 * above it, and nothing showed which module a scope belonged to.
 *
 * Grouping comes from the server's module catalogue, the same one the role editor's checkboxes use. Every registered
 * scope belongs to exactly one group, pinned server-side, so nothing is dropped — but a group the role holds nothing
 * from is omitted rather than rendered empty.
 */
const PermissionScopeSummary = ({permissionScopeGroups, scopes}: PermissionScopeSummaryProps) => {
    const groupedPermissions = useMemo(() => {
        const heldScopes = new Set(scopes);

        return permissionScopeGroups
            .map((permissionScopeGroup) => ({
                actions: permissionScopeGroup.scopes
                    .filter((scope) => heldScopes.has(scope))
                    .map((scope) => getScopeActionLabel(permissionScopeGroup.name, scope))
                    .join(', '),
                name: getRoleLabel(permissionScopeGroup.name),
            }))
            .filter((permissionScopeGroup) => permissionScopeGroup.actions !== '');
    }, [permissionScopeGroups, scopes]);

    return (
        <div className="grid gap-3 sm:grid-cols-2">
            {groupedPermissions.map((permissionScopeGroup) => (
                <div key={permissionScopeGroup.name}>
                    <h4 className="text-xs font-semibold tracking-wide text-content-neutral-secondary uppercase">
                        {permissionScopeGroup.name}
                    </h4>

                    <p className="text-sm">{permissionScopeGroup.actions}</p>
                </div>
            ))}
        </div>
    );
};

export default PermissionScopeSummary;
