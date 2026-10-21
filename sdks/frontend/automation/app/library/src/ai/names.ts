export function toFlatKey(key: string): string {
    return key.replace(/[^A-Za-z0-9]+/g, '_').toUpperCase();
}
