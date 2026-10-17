import {type Page, expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {type WorkflowDeployerI} from '../../fixtures/workflowDeployment';
import {SERVER_URL} from '../../utils/apiUtils';
import {type ApprovalRunI, deployAndStartApprovalRun, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    getShortId,
    mintShortToken,
    tamperTokenExpiry,
    tamperTokenSignature,
    waitForPendingApproval,
} from '../../utils/approvalUtils';
import {ALL_IN_PLACE_CHANNELS, buildApprovalWorkflow} from '../../utils/approvalWorkflowDefinitions';
import {waitAndStaySignedIn} from '../../utils/auth';
import {
    buildDiscordButtonInteraction,
    postDiscordInteraction,
    postMattermostAction,
    postSlackInteraction,
    postTelegramCallback,
    postWhatsAppButtonReply,
} from '../../utils/channelCallbackUtils';
import {getJob, getTaskOutput, stopJob, waitForJobStatus} from '../../utils/jobUtils';
import {type RequestCatcherI, startRequestCatcher} from '../../utils/requestCatcher';
import {
    SLOW_TAG,
    skipUnlessDiscordKeyIsSet,
    skipUnlessSlowRunEnabled,
    skipUnlessWhatsAppVerifyTokenIsSet,
} from '../../utils/testEnvironment';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

const ONE_HOUR = 60 * 60 * 1000;

let requestCatcher: RequestCatcherI;

async function startChannelsRun(deployer: WorkflowDeployerI, page: Page, expiresIn = 2): Promise<ApprovalRunI> {
    return deployAndStartApprovalRun({
        body: {n: uniqueSuffix()},
        definition: buildApprovalWorkflow({
            channels: ALL_IN_PLACE_CHANNELS,
            expiresIn,
            formTitle: `B channels ${uniqueSuffix()}`,
            label: `B channels ${uniqueSuffix()}`,
        }),
        deployer,
        page,
    });
}

function slackResponseUrl(clickName: string): string {
    return `${requestCatcher.baseUrl}/slack/${clickName}`;
}

async function expectApprovalOutput(page: Page, jobId: string, expectedOutput: Record<string, unknown>) {
    await waitForJobStatus(page, jobId, 'COMPLETED');

    expect(await getTaskOutput(page, jobId, 'approval_1')).toMatchObject(expectedOutput);
}

test.describe('In-place channel interactivity', () => {
    test.beforeAll(async () => {
        requestCatcher = await startRequestCatcher();
    });

    test.afterAll(async () => {
        await requestCatcher.close();
    });

    test('B1 Slack button approve', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);
        const clickName = `b1-${uniqueSuffix()}`;

        await test.step('A signed block_actions callback approves in place', async () => {
            const slackResponse = await postSlackInteraction({
                action: 'approve',
                request,
                responseUrl: slackResponseUrl(clickName),
                token: approvalRun.token,
                userName: 'alice',
            });

            expect(slackResponse.status()).toBe(200);

            await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {
                approved: true,
                approvedBy: '@alice',
                comment: '',
            });
        });

        await test.step('Slack receives the rewritten message', async () => {
            const caughtRequest = await requestCatcher.waitForRequest(`/slack/${clickName}`);

            expect(caughtRequest.body).toContain(':white_check_mark: Approved by @alice.');
        });
    });

    test('B2 Slack rejects a bad signature or a stale timestamp', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);

        const callbackProps = {
            action: 'approve' as const,
            request,
            responseUrl: slackResponseUrl(`b2-${uniqueSuffix()}`),
            token: approvalRun.token,
            userName: 'mallory',
        };

        await test.step('Wrong secret, stale timestamp and a missing signature answer 401', async () => {
            const wrongSecretResponse = await postSlackInteraction({...callbackProps, signingSecret: 'wrong-secret'});

            expect(wrongSecretResponse.status()).toBe(401);

            const staleResponse = await postSlackInteraction({
                ...callbackProps,
                timestamp: String(Math.floor(Date.now() / 1000) - 600),
            });

            expect(staleResponse.status()).toBe(401);

            const unsignedResponse = await postSlackInteraction({...callbackProps, withSignature: false});

            expect(unsignedResponse.status()).toBe(401);
        });

        await test.step('The run is still waiting', async () => {
            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });
    });

    test('B3 Slack discard without a bot token discards without a comment', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);
        const clickName = `b3-${uniqueSuffix()}`;

        const slackResponse = await postSlackInteraction({
            action: 'discard',
            request,
            responseUrl: slackResponseUrl(clickName),
            token: approvalRun.token,
            triggerId: 't1',
            userName: 'carol',
        });

        expect(slackResponse.status()).toBe(200);

        await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: false, approvedBy: '@carol'});

        const caughtRequest = await requestCatcher.waitForRequest(`/slack/${clickName}`);

        expect(caughtRequest.body).toContain(':no_entry_sign: Discarded by @carol.');
    });

    test('B4 Telegram callback with the webhook secret token', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);
        const shortId = await getShortId(request, approvalRun.token);

        await test.step('A wrong or missing secret token answers 401', async () => {
            const wrongSecretResponse = await postTelegramCallback({
                callbackData: `${shortId}:a`,
                request,
                secretToken: 'wrong-secret',
            });

            expect(wrongSecretResponse.status()).toBe(401);

            const missingSecretResponse = await postTelegramCallback({
                callbackData: `${shortId}:a`,
                request,
                secretToken: null,
            });

            expect(missingSecretResponse.status()).toBe(401);

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('The right secret token approves in place', async () => {
            const telegramResponse = await postTelegramCallback({callbackData: `${shortId}:a`, request});

            expect(telegramResponse.status()).toBe(200);

            await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: true, approvedBy: '@bob'});
        });

        await test.step('An unknown short id is acknowledged and ignored', async () => {
            const unknownResponse = await postTelegramCallback({callbackData: 'unknownShortId123:a', request});

            expect(unknownResponse.status()).toBe(200);
        });
    });

    test('B5 Discord interaction signed with Ed25519', async ({authenticatedPage, deployer, request}) => {
        skipUnlessDiscordKeyIsSet();

        await test.step('A signed PING answers PONG, a bad signature 401', async () => {
            const pingResponse = await postDiscordInteraction({body: {type: 1}, request});

            expect(pingResponse.status()).toBe(200);
            expect(await pingResponse.json()).toMatchObject({type: 1});

            const badPingResponse = await postDiscordInteraction({body: {type: 1}, request, tamperSignature: true});

            expect(badPingResponse.status()).toBe(401);
        });

        const approvalRun = await startChannelsRun(deployer, authenticatedPage);
        const shortId = await getShortId(request, approvalRun.token);

        await test.step('A badly signed button leaves the run alone', async () => {
            const badButtonResponse = await postDiscordInteraction({
                body: buildDiscordButtonInteraction(`${shortId}:a`, 'dave'),
                request,
                tamperSignature: true,
            });

            expect(badButtonResponse.status()).toBe(401);

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('A signed button approves in place and updates the message', async () => {
            const buttonResponse = await postDiscordInteraction({
                body: buildDiscordButtonInteraction(`${shortId}:a`, 'dave'),
                request,
            });

            expect(buttonResponse.status()).toBe(200);

            const buttonReply = await buttonResponse.json();

            expect(buttonReply.type).toBe(7);
            expect(JSON.stringify(buttonReply)).toContain('✅ Approved by @dave.');

            await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: true, approvedBy: '@dave'});
        });
    });

    test('B6 WhatsApp verify handshake', async ({request}) => {
        skipUnlessWhatsAppVerifyTokenIsSet();

        const verifyResponse = await request.get(`${SERVER_URL}/whatsapp/interactivity`, {
            params: {
                'hub.challenge': 'abc',
                'hub.mode': 'subscribe',
                'hub.verify_token': process.env.BYTECHEF_WHATSAPP_VERIFY_TOKEN as string,
            },
        });

        expect(verifyResponse.status()).toBe(200);
        expect(await verifyResponse.text()).toBe('abc');

        const wrongTokenResponse = await request.get(`${SERVER_URL}/whatsapp/interactivity`, {
            params: {'hub.challenge': 'abc', 'hub.mode': 'subscribe', 'hub.verify_token': 'wrong-token'},
        });

        expect(wrongTokenResponse.status()).toBe(403);
    });

    test('B6 WhatsApp (Meta) button reply', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);

        await test.step('A wrong or missing HMAC answers 401', async () => {
            const wrongSignatureResponse = await postWhatsAppButtonReply({
                appSecret: 'wrong-secret',
                buttonId: `a:${approvalRun.token}`,
                contactName: 'Dave',
                request,
            });

            expect(wrongSignatureResponse.status()).toBe(401);

            const unsignedResponse = await postWhatsAppButtonReply({
                buttonId: `a:${approvalRun.token}`,
                contactName: 'Dave',
                request,
                withSignature: false,
            });

            expect(unsignedResponse.status()).toBe(401);

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('A signed button reply approves in place', async () => {
            const buttonResponse = await postWhatsAppButtonReply({
                buttonId: `a:${approvalRun.token}`,
                contactName: 'Dave',
                request,
            });

            expect(buttonResponse.status()).toBe(200);

            await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: true, approvedBy: 'Dave'});
        });

        await test.step('Clicking again is acknowledged without changing the outcome', async () => {
            const repeatedResponse = await postWhatsAppButtonReply({
                buttonId: `d:${approvalRun.token}`,
                contactName: 'Dave',
                request,
            });

            expect(repeatedResponse.status()).toBe(200);

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'approval_1')).toMatchObject({
                approved: true,
            });
        });
    });

    test('B7 Mattermost button', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);

        await test.step('A callback without context is answered as malformed', async () => {
            const malformedResponse = await postMattermostAction({request, userName: 'erin'});

            expect((await malformedResponse.text()).toLowerCase()).toContain('malformed');

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('The integration context approves in place, with an empty approvedBy', async () => {
            const mattermostResponse = await postMattermostAction({
                request,
                token: approvalRun.token,
                userName: 'erin',
            });

            expect(mattermostResponse.status()).toBe(200);
            expect(await mattermostResponse.json()).toEqual({update: {message: ':white_check_mark: Approved.'}});

            await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: true, approvedBy: ''});
        });
    });

    test('B8 short-token minting', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);

        await test.step('A signed token mints the same short id every time', async () => {
            const firstShortId = await getShortId(request, approvalRun.token);
            const secondShortId = await getShortId(request, approvalRun.token);

            expect(firstShortId).toBeTruthy();
            expect(secondShortId).toBe(firstShortId);
        });

        await test.step('Tampered, garbage and empty bodies answer 400', async () => {
            for (const invalidBody of [
                tamperTokenSignature(approvalRun.token),
                tamperTokenExpiry(approvalRun.token),
                'garbage',
                '',
            ]) {
                expect((await mintShortToken(request, invalidBody)).status(), invalidBody).toBe(400);
            }
        });

        await test.step('A JSON body answers 415', async () => {
            const jsonResponse = await mintShortToken(
                request,
                JSON.stringify({token: approvalRun.token}),
                'application/json'
            );

            expect(jsonResponse.status()).toBe(415);
        });
    });

    test('B9 approval endpoints are answered by their controllers without a session', async ({request}) => {
        const anonymousCalls = [
            {
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                path: '/slack/interactivity',
                rawBody: 'payload=%7B%7D',
            },
            {headers: {'Content-Type': 'application/json'}, path: '/telegram/interactivity', rawBody: '{}'},
            {headers: {'Content-Type': 'application/json'}, path: '/whatsapp/interactivity', rawBody: '{}'},
            {headers: {'Content-Type': 'application/json'}, path: '/mattermost/interactivity', rawBody: '{}'},
            {headers: {'Content-Type': 'application/json'}, path: '/discord/interactivity', rawBody: '{"type":1}'},
            {headers: {'Content-Type': 'text/plain'}, path: '/approval/short-token', rawBody: 'bogus'},
            {headers: {'Content-Type': 'application/json'}, path: '/job/resume/bogus', rawBody: '{"approved":true}'},
        ];

        for (const anonymousCall of anonymousCalls) {
            const response = await request.post(`${SERVER_URL}${anonymousCall.path}`, {
                data: anonymousCall.rawBody,
                headers: anonymousCall.headers,
                maxRedirects: 0,
            });

            expect(response.status(), anonymousCall.path).not.toBe(403);
            expect([200, 400, 401, 404, 415], anonymousCall.path).toContain(response.status());
        }

        const approvalFormResponse = await request.get(`${SERVER_URL}/api/platform/internal/approval-form/bogus`, {
            maxRedirects: 0,
        });

        expect(approvalFormResponse.status()).not.toBe(403);
        expect([400, 404]).toContain(approvalFormResponse.status());

        const controlResponse = await request.get(`${SERVER_URL}/api/automation/internal/projects`, {
            maxRedirects: 0,
        });

        expect([401, 403]).toContain(controlResponse.status());
    });

    test('B10 Slack signature over a raw body with percent-encoded spaces', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const approvalRun = await startChannelsRun(deployer, authenticatedPage);
        const userName = "bob x*~()!'é";

        const slackResponse = await postSlackInteraction({
            action: 'approve',
            percentEncodeSpaces: true,
            request,
            responseUrl: slackResponseUrl(`b10-${uniqueSuffix()}`),
            token: approvalRun.token,
            userName,
        });

        expect(slackResponse.status()).toBe(200);

        await expectApprovalOutput(authenticatedPage, approvalRun.jobId, {approved: true, approvedBy: `@${userName}`});
    });

    test('B11 clicking again after resolution or stop changes nothing', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const resolvedRun = await startChannelsRun(deployer, authenticatedPage);
        const resolvedShortId = await getShortId(request, resolvedRun.token);

        await test.step('Resolve the run through Slack', async () => {
            const slackResponse = await postSlackInteraction({
                action: 'approve',
                request,
                responseUrl: slackResponseUrl(`b11-first-${uniqueSuffix()}`),
                token: resolvedRun.token,
                userName: 'alice',
            });

            expect(slackResponse.status()).toBe(200);

            await expectApprovalOutput(authenticatedPage, resolvedRun.jobId, {approved: true, approvedBy: '@alice'});
        });

        await test.step('Every channel reports the approval as already resolved', async () => {
            const slackClickName = `b11-again-${uniqueSuffix()}`;

            await postSlackInteraction({
                action: 'discard',
                request,
                responseUrl: slackResponseUrl(slackClickName),
                token: resolvedRun.token,
                userName: 'alice',
            });

            const slackReply = await requestCatcher.waitForRequest(`/slack/${slackClickName}`);

            expect(slackReply.body).toContain(':information_source: This approval was already resolved.');
            expect(slackReply.body).not.toContain('expired');

            const mattermostResponse = await postMattermostAction({
                approved: false,
                request,
                token: resolvedRun.token,
                userName: 'erin',
            });

            expect(await mattermostResponse.text()).toContain(
                ':information_source: This approval was already resolved.'
            );

            if (process.env.BYTECHEF_DISCORD_PRIVATE_KEY_PEM) {
                const discordResponse = await postDiscordInteraction({
                    body: buildDiscordButtonInteraction(`${resolvedShortId}:d`, 'dave'),
                    request,
                });

                expect(JSON.stringify(await discordResponse.json())).toContain(
                    'ℹ️ This approval was already resolved.'
                );
            }

            expect((await postTelegramCallback({callbackData: `${resolvedShortId}:d`, request})).status()).toBe(200);

            expect(
                (
                    await postWhatsAppButtonReply({
                        buttonId: `d:${resolvedRun.token}`,
                        contactName: 'Dave',
                        request,
                    })
                ).status()
            ).toBe(200);

            expect(await getTaskOutput(authenticatedPage, resolvedRun.jobId, 'approval_1')).toMatchObject({
                approved: true,
                approvedBy: '@alice',
            });
        });

        await test.step('On a stopped run every channel reports the approval as no longer available', async () => {
            const stoppedRun = await startChannelsRun(deployer, authenticatedPage);
            const stoppedShortId = await getShortId(request, stoppedRun.token);

            expect((await stopJob(authenticatedPage, stoppedRun.jobId)).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, stoppedRun.jobId, 'CANCELLED');

            const slackClickName = `b11-stopped-${uniqueSuffix()}`;

            await postSlackInteraction({
                action: 'approve',
                request,
                responseUrl: slackResponseUrl(slackClickName),
                token: stoppedRun.token,
                userName: 'alice',
            });

            const slackReply = await requestCatcher.waitForRequest(`/slack/${slackClickName}`);

            expect(slackReply.body).toContain(':warning: This approval is no longer available.');

            if (process.env.BYTECHEF_DISCORD_PRIVATE_KEY_PEM) {
                const discordResponse = await postDiscordInteraction({
                    body: buildDiscordButtonInteraction(`${stoppedShortId}:a`, 'dave'),
                    request,
                });

                expect(JSON.stringify(await discordResponse.json())).toContain(
                    '⚠️ This approval is no longer available.'
                );
            }

            expect((await getJob(authenticatedPage, stoppedRun.jobId)).status).toBe('CANCELLED');
        });
    });

    test(`B11 clicking after expiry reports the approval as expired ${SLOW_TAG}`, async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        skipUnlessSlowRunEnabled();

        test.setTimeout(ONE_HOUR + 30 * 60 * 1000);

        const expiringRun = await startChannelsRun(deployer, authenticatedPage, 1);
        const expiringShortId = await getShortId(request, expiringRun.token);

        const pendingApproval = await waitForPendingApproval(authenticatedPage, expiringRun.jobId);

        await waitAndStaySignedIn(
            authenticatedPage,
            Date.parse(pendingApproval.expiresAt as string) - Date.now() + 5000
        );

        const slackClickName = `b11-expired-${uniqueSuffix()}`;

        await postSlackInteraction({
            action: 'approve',
            request,
            responseUrl: slackResponseUrl(slackClickName),
            token: expiringRun.token,
            userName: 'alice',
        });

        const slackReply = await requestCatcher.waitForRequest(`/slack/${slackClickName}`);

        expect(slackReply.body).toContain(':hourglass: This approval expired before it was resolved.');

        if (process.env.BYTECHEF_DISCORD_PRIVATE_KEY_PEM) {
            const discordResponse = await postDiscordInteraction({
                body: buildDiscordButtonInteraction(`${expiringShortId}:a`, 'dave'),
                request,
            });

            expect(JSON.stringify(await discordResponse.json())).toContain('⌛');
        }

        await waitForJobStatus(authenticatedPage, expiringRun.jobId, 'FAILED');
    });
});
