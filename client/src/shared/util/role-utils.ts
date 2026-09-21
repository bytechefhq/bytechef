export const getRoleLabel = (role: string): string =>
    role
        .replace(/^ROLE_/, '')
        .split('_')
        .filter(Boolean)
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
