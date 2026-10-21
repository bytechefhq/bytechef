const RANDOM_BYTE_COUNT = 32;

const VERIFIER_PATTERN = /^[A-Za-z0-9\-._~]{43,128}$/;

function toBase64Url(bytes: Uint8Array): string {
    let binary = '';

    for (const byte of bytes) {
        binary += String.fromCharCode(byte);
    }

    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function randomBase64Url(): string {
    return toBase64Url(globalThis.crypto.getRandomValues(new Uint8Array(RANDOM_BYTE_COUNT)));
}

export function generateCodeVerifier(): string {
    return randomBase64Url();
}

export function generateState(): string {
    return randomBase64Url();
}

export async function createCodeChallenge(codeVerifier: string): Promise<string> {
    if (!VERIFIER_PATTERN.test(codeVerifier)) {
        throw new RangeError('A PKCE code verifier is 43 to 128 characters from A-Z, a-z, 0-9, "-", ".", "_" and "~"');
    }

    const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(codeVerifier));

    return toBase64Url(new Uint8Array(digest));
}

/**
 * Compares the `state` kept for a pending sign-in with the one the authorization server sent back, in time that does
 * not depend on where they differ. An empty or missing value never matches.
 */
export function isSameState(expected: string, returned: string | null | undefined): boolean {
    if (!expected || !returned) {
        return false;
    }

    const encoder = new TextEncoder();
    const expectedBytes = encoder.encode(expected);
    const returnedBytes = encoder.encode(returned);
    const length = Math.max(expectedBytes.length, returnedBytes.length);
    let difference = expectedBytes.length ^ returnedBytes.length;

    for (let index = 0; index < length; index++) {
        difference |= (expectedBytes[index] ?? 0) ^ (returnedBytes[index] ?? 0);
    }

    return difference === 0;
}
