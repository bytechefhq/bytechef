import {Accordion, AccordionContent, AccordionItem, AccordionTrigger} from '@/components/ui/accordion';
import PermissionScopeSummary from '@/ee/shared/components/custom-roles/components/PermissionScopeSummary';
import {useBuiltInRolesQuery} from '@/shared/middleware/graphql';
import {getRoleLabel} from '@/shared/util/role-utils';
import {useMemo} from 'react';

interface PermissionScopeGroupI {
    name: string;
    scopes: string[];
}

interface BuiltInRolesProps {
    permissionScopeGroups: PermissionScopeGroupI[];
}

/**
 * The fixed role tiers, read-only, above the custom roles they exist to make unnecessary. An operator who cannot see
 * what EDITOR already grants writes a custom role that restates it — and then has to maintain it forever.
 *
 * Collapsed by default: ADMIN holds every scope the server was built with, so expanded by default this section would
 * push the custom roles it is meant to inform off the screen.
 */
const BuiltInRoles = ({permissionScopeGroups}: BuiltInRolesProps) => {
    const {data: builtInRolesData} = useBuiltInRolesQuery({});

    const builtInRoles = useMemo(() => builtInRolesData?.builtInRoles ?? [], [builtInRolesData]);

    if (builtInRoles.length === 0) {
        return null;
    }

    return (
        <fieldset className="border-0 p-0">
            <p className="text-sm text-content-neutral-secondary">
                Every member holds one of these. Their permissions are fixed — create a custom role only when a member
                needs a set none of them covers.
            </p>

            <Accordion className="mt-3 rounded-md border" type="multiple">
                {builtInRoles.map((builtInRole) => (
                    <AccordionItem key={builtInRole.name} value={builtInRole.name}>
                        <AccordionTrigger className="items-center px-4">
                            <span className="flex items-center gap-3">
                                {getRoleLabel(builtInRole.name)}

                                <span className="text-xs font-normal text-content-neutral-secondary">
                                    {builtInRole.scopes.length} permissions
                                </span>
                            </span>
                        </AccordionTrigger>

                        <AccordionContent className="px-4">
                            <PermissionScopeSummary
                                permissionScopeGroups={permissionScopeGroups}
                                scopes={builtInRole.scopes}
                            />
                        </AccordionContent>
                    </AccordionItem>
                ))}
            </Accordion>
        </fieldset>
    );
};

export default BuiltInRoles;
