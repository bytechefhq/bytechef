export function generateRandomId(): string {
    return Array(32)
        .fill(0)
        .map(() => Math.random().toString(36).charAt(2))
        .join('');
}

export function getRandomId() {
    return new Date().getTime().toString(36) + Math.random().toString(36).slice(2);
}
