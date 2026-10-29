import {expect} from '@playwright/test';
import {type IncomingMessage, type Server, type ServerResponse, createServer} from 'node:http';
import {type AddressInfo} from 'node:net';

export interface CaughtRequestI {
    body: string;
    path: string;
}

export interface RequestCatcherI {
    baseUrl: string;
    caughtRequests: CaughtRequestI[];
    close: () => Promise<void>;
    waitForRequest: (pathPrefix: string, timeout?: number) => Promise<CaughtRequestI>;
}

/**
 * A local HTTP listener that answers 200 to every POST and records it. It stands in for Slack's `response_url` and
 * for the service a `wait/v1/onWebhookCall` step posts its resume URL to.
 */
export async function startRequestCatcher(): Promise<RequestCatcherI> {
    const caughtRequests: CaughtRequestI[] = [];

    const server: Server = createServer((incomingMessage: IncomingMessage, serverResponse: ServerResponse) => {
        const bodyChunks: Buffer[] = [];

        incomingMessage.on('data', (bodyChunk: Buffer) => bodyChunks.push(bodyChunk));

        incomingMessage.on('end', () => {
            caughtRequests.push({body: Buffer.concat(bodyChunks).toString('utf8'), path: incomingMessage.url ?? ''});

            serverResponse.writeHead(200, {'Content-Type': 'application/json'});

            serverResponse.end('{"ok":true}');
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
        waitForRequest,
    };
}
