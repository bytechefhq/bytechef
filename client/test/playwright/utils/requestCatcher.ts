import {expect} from '@playwright/test';
import {type IncomingMessage, type Server, type ServerResponse, createServer} from 'node:http';
import {type AddressInfo} from 'node:net';

export interface CaughtRequestI {
    body: string;
    headers: IncomingMessage['headers'];
    path: string;
}

interface CatcherResponseI {
    body: string;
    status: number;
}

export interface RequestCatcherI {
    baseUrl: string;
    caughtRequests: CaughtRequestI[];
    close: () => Promise<void>;
    respondWith: (pathPrefix: string, status: number, body: string) => void;
    waitForRequest: (pathPrefix: string, timeout?: number) => Promise<CaughtRequestI>;
}

export async function startRequestCatcher(): Promise<RequestCatcherI> {
    const caughtRequests: CaughtRequestI[] = [];
    const responsesByPathPrefix = new Map<string, CatcherResponseI>();

    const server: Server = createServer((incomingMessage: IncomingMessage, serverResponse: ServerResponse) => {
        const bodyChunks: Buffer[] = [];

        incomingMessage.on('data', (bodyChunk: Buffer) => bodyChunks.push(bodyChunk));

        incomingMessage.on('end', () => {
            const path = incomingMessage.url ?? '';

            caughtRequests.push({
                body: Buffer.concat(bodyChunks).toString('utf8'),
                headers: incomingMessage.headers,
                path,
            });

            const matchingPathPrefix = [...responsesByPathPrefix.keys()].find((pathPrefix) =>
                path.startsWith(pathPrefix)
            );

            const catcherResponse = (matchingPathPrefix && responsesByPathPrefix.get(matchingPathPrefix)) || {
                body: '{"ok":true}',
                status: 200,
            };

            serverResponse.writeHead(catcherResponse.status, {'Content-Type': 'application/json'});

            serverResponse.end(catcherResponse.body);
        });
    });

    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));

    const {port} = server.address() as AddressInfo;

    const waitForRequest = async (pathPrefix: string, timeout = 30000): Promise<CaughtRequestI> => {
        let caughtRequest: CaughtRequestI | undefined;

        await expect(() => {
            caughtRequest = caughtRequests.find((request) => request.path.startsWith(pathPrefix));

            expect(caughtRequest).toBeTruthy();
        }).toPass({timeout});

        return caughtRequest as CaughtRequestI;
    };

    return {
        baseUrl: `http://127.0.0.1:${port}`,
        caughtRequests,
        close: () => new Promise<void>((resolve) => server.close(() => resolve())),
        respondWith: (pathPrefix, status, body) => responsesByPathPrefix.set(pathPrefix, {body, status}),
        waitForRequest,
    };
}
