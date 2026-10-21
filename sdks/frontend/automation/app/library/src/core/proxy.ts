import type {components} from '../generated/openapi';
import type {CallOptions} from './connections';
import {BytechefAppError} from './errors';
import type {Requester} from './request';
import {expectObject} from './response';
import type {ProxyBody, ProxyRequest, ProxyResult} from './types';

type RawProxyResponse = components['schemas']['ProxyResponse'];

function bytesToBase64(bytes: Uint8Array): string {
    let binary = '';

    for (let index = 0; index < bytes.length; index += 0x8000) {
        binary += String.fromCharCode(...bytes.subarray(index, index + 0x8000));
    }

    return btoa(binary);
}

function base64ToBytes(base64: string, status: number): Uint8Array {
    let binary: string;

    try {
        binary = atob(base64);
    } catch (error) {
        throw new BytechefAppError(
            'INVALID_RESPONSE',
            'ByteChef answered a proxy body marked BASE64 that is not valid base64',
            {cause: error, status}
        );
    }

    const bytes = new Uint8Array(binary.length);

    for (let index = 0; index < binary.length; index += 1) {
        bytes[index] = binary.charCodeAt(index);
    }

    return bytes;
}

function binaryBytes(body: ProxyBody): Uint8Array | undefined {
    if (ArrayBuffer.isView(body)) {
        return new Uint8Array(body.buffer, body.byteOffset, body.byteLength);
    }

    if (Object.prototype.toString.call(body) === '[object ArrayBuffer]') {
        return new Uint8Array(body as ArrayBuffer);
    }

    return undefined;
}

export function encodeProxyBody(
    body: ProxyBody | undefined,
    contentType: string | undefined
): {body?: string; bodyEncoding?: 'BASE64' | 'UTF8'; contentType?: string} {
    if (body === undefined) {
        return contentType === undefined ? {} : {contentType};
    }

    if (typeof body === 'string') {
        return contentType === undefined ? {body, bodyEncoding: 'UTF8'} : {body, bodyEncoding: 'UTF8', contentType};
    }

    const bytes = binaryBytes(body);

    if (bytes !== undefined) {
        return {
            body: bytesToBase64(bytes),
            bodyEncoding: 'BASE64',
            contentType: contentType ?? 'application/octet-stream',
        };
    }

    return {body: JSON.stringify(body), bodyEncoding: 'UTF8', contentType: contentType ?? 'application/json'};
}

function toResult(raw: RawProxyResponse, status: number): ProxyResult {
    const body = raw.body ?? null;
    const base64Encoded = raw.bodyEncoding === 'BASE64';
    let decodedBytes: Uint8Array | undefined = body !== null && base64Encoded ? base64ToBytes(body, status) : undefined;
    let decodedText: string | undefined = body === null ? '' : base64Encoded ? undefined : body;

    function bodyBytes(): Uint8Array {
        decodedBytes ??= new TextEncoder().encode(decodedText ?? '');

        return decodedBytes;
    }

    function bodyText(): string {
        decodedText ??= new TextDecoder().decode(bodyBytes());

        return decodedText;
    }

    return {
        body,
        bodyEncoding: raw.bodyEncoding,
        bytes: () => bodyBytes().slice(),
        headers: Object.fromEntries(Object.entries(raw.headers ?? {}).map(([name, values]) => [name, [...values]])),
        json: <T = unknown>(): T => JSON.parse(bodyText()) as T,
        status: raw.status,
        text: bodyText,
    };
}

export function createProxyApi(request: Requester) {
    return async (bindingKey: string, proxyRequest: ProxyRequest, options: CallOptions = {}): Promise<ProxyResult> => {
        const {body, contentType, ...rest} = proxyRequest;
        const path = `/connections/${encodeURIComponent(bindingKey)}/proxy`;
        const result = await request({
            body: {...rest, ...encodeProxyBody(body, contentType)},
            method: 'POST',
            path,
            signal: options.signal,
        });

        return toResult(expectObject<RawProxyResponse>(result, path), result.status);
    };
}
