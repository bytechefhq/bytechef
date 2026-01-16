export function generatePassword(length = 12) {
    const minLength = Math.max(8, length);
    const uppercaseLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const lowercaseLetters = 'abcdefghijklmnopqrstuvwxyz';
    const digits = '0123456789';
    const allCharacters = uppercaseLetters + lowercaseLetters + digits;

    const bytes = new Uint32Array(minLength + 2);

    crypto.getRandomValues(bytes);

    const requiredCharacters = [uppercaseLetters[bytes[0] % uppercaseLetters.length], digits[bytes[1] % digits.length]];
    const characters: string[] = [];

    for (let i = 2; i < minLength; i++) {
        characters.push(allCharacters[bytes[i] % allCharacters.length]);
    }

    characters.splice(bytes[minLength] % (characters.length + 1), 0, requiredCharacters[0]);
    characters.splice(bytes[minLength + 1] % (characters.length + 1), 0, requiredCharacters[1]);

    return characters.join('');
}

export function isValidPassword(password: string) {
    return password.length >= 8 && /[A-Z]/.test(password) && /\d/.test(password);
}
