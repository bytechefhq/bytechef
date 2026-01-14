export function generatePassword(length = 12) {
    // Must be at least 8 characters, include at least 1 number and 1 uppercase
    const minLen = Math.max(8, length);
    const U = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const L = 'abcdefghijklmnopqrstuvwxyz';
    const D = '0123456789';
    const ALL = U + L + D;

    // Generate extra bytes for splice positions to avoid reusing character-generation bytes
    const bytes = new Uint32Array(minLen + 2);
    crypto.getRandomValues(bytes);

    // Ensure required characters
    const required = [U[bytes[0] % U.length], D[bytes[1] % D.length]];
    const chars: string[] = [];

    for (let i = 2; i < minLen; i++) {
        chars.push(ALL[bytes[i] % ALL.length]);
    }

    // Insert required at random positions using dedicated bytes
    chars.splice(bytes[minLen] % (chars.length + 1), 0, required[0]);
    chars.splice(bytes[minLen + 1] % (chars.length + 1), 0, required[1]);

    return chars.join('');
}

export function isValidPassword(password: string) {
    return password.length >= 8 && /[A-Z]/.test(password) && /\d/.test(password);
}
