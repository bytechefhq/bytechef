import {WorkspaceRole} from '@/shared/middleware/graphql';

/**
 * Prefix distinguishing a custom role from a built-in one inside a single Select. The two live in different columns
 * server-side and are set by different arguments, so the option value has to say which it is.
 */
export const CUSTOM_ROLE_PREFIX = 'custom:';

/**
 * Splits a picker value into the two mutually exclusive arguments the invite/add mutations take. The server enforces
 * the same XOR, so sending both — or neither — is rejected rather than silently resolved.
 */
export const toRoleArguments = (value: string) =>
    value.startsWith(CUSTOM_ROLE_PREFIX)
        ? {customRoleId: value.slice(CUSTOM_ROLE_PREFIX.length), role: undefined}
        : {customRoleId: undefined, role: value as WorkspaceRole};
