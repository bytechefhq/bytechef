import CustomRolesManager from '@/ee/shared/components/custom-roles/CustomRolesManager';

/**
 * Custom roles are tenant-global: defined once here, assignable in every workspace. The route's ROLE_ADMIN gate
 * matches the server's isTenantAdmin() check on every mutation — the server's check is the one that counts.
 */
const GlobalCustomRoles = () => <CustomRolesManager />;

export default GlobalCustomRoles;
