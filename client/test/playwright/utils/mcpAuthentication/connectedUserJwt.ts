import {createSign, generateKeyPairSync} from 'node:crypto';

interface ConnectedUserJwtProps {
    keyId: string;
    privateKeyPem: string;
    subject: string;
}

function encodeBase64Url(value: Buffer | string): string {
    return Buffer.from(value).toString('base64url');
}

function createSigningInput(algorithm: string, keyId: string, subject: string): string {
    const issuedAt = Math.floor(Date.now() / 1000);

    const header = {alg: algorithm, kid: keyId, typ: 'JWT'};
    const payload = {exp: issuedAt + 600, iat: issuedAt, sub: subject};

    return `${encodeBase64Url(JSON.stringify(header))}.${encodeBase64Url(JSON.stringify(payload))}`;
}

export function signConnectedUserJwt({keyId, privateKeyPem, subject}: ConnectedUserJwtProps): string {
    const signingInput = createSigningInput('RS256', keyId, subject);

    const signer = createSign('RSA-SHA256');

    signer.update(signingInput);

    return `${signingInput}.${encodeBase64Url(signer.sign(privateKeyPem))}`;
}

export function createUnsignedConnectedUserJwt(keyId: string, subject: string): string {
    return `${createSigningInput('none', keyId, subject)}.`;
}

export function generateUnrelatedPrivateKeyPem(): string {
    const {privateKey} = generateKeyPairSync('rsa', {modulusLength: 2048});

    return privateKey.export({format: 'pem', type: 'pkcs8'}).toString();
}
