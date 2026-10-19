import {type APIRequestContext, type APIResponse} from '@playwright/test';
import {createHmac, createPrivateKey, createPublicKey, sign} from 'node:crypto';
import {existsSync, readFileSync} from 'node:fs';

import {SERVER_URL} from './apiUtils';

/**
 * The secrets the test connections are created with. The tests play Slack, Telegram and WhatsApp themselves, so
 * they sign each callback with the same secret the connection holds.
 */
export const CHANNEL_SECRETS = {
    slackSigningSecret: 'playwright-slack-signing-secret',
    telegramWebhookSecretToken: 'playwright-telegram-secret',
    whatsAppAppSecret: 'playwright-whatsapp-app-secret',
} as const;

const PLACEHOLDER_DISCORD_PUBLIC_KEY = '0'.repeat(64);

export function getDiscordPrivateKeyPem(): string | undefined {
    const privateKeyPath = process.env.BYTECHEF_DISCORD_PRIVATE_KEY_PEM;

    if (!privateKeyPath || !existsSync(privateKeyPath)) {
        return undefined;
    }

    return readFileSync(privateKeyPath, 'utf8');
}

export function getDiscordPublicKeyHex(): string {
    const privateKeyPem = getDiscordPrivateKeyPem();

    if (!privateKeyPem) {
        return PLACEHOLDER_DISCORD_PUBLIC_KEY;
    }

    const publicKeyDer = createPublicKey(createPrivateKey(privateKeyPem)).export({format: 'der', type: 'spki'});

    return publicKeyDer.subarray(publicKeyDer.length - 32).toString('hex');
}

function currentEpochSeconds(): string {
    return String(Math.floor(Date.now() / 1000));
}

export type SlackActionType = 'approve' | 'discard';

interface SlackCallbackProps {
    action: SlackActionType;
    percentEncodeSpaces?: boolean;
    request: APIRequestContext;
    responseUrl: string;
    signingSecret?: string;
    timestamp?: string;
    token: string;
    triggerId?: string;
    userName: string;
    withSignature?: boolean;
}

/**
 * Posts a Slack `block_actions` interaction the way Slack does: a form-encoded `payload` field signed with
 * `v0=HMAC-SHA256(signingSecret, "v0:<timestamp>:<raw body>")`.
 */
export async function postSlackInteraction({
    action,
    percentEncodeSpaces = false,
    request,
    responseUrl,
    signingSecret = CHANNEL_SECRETS.slackSigningSecret,
    timestamp = currentEpochSeconds(),
    token,
    triggerId,
    userName,
    withSignature = true,
}: SlackCallbackProps): Promise<APIResponse> {
    const payload = {
        actions: [{action_id: `approval_${action}`, value: token}],
        response_url: responseUrl,
        ...(triggerId ? {trigger_id: triggerId} : {}),
        type: 'block_actions',
        user: {id: 'U1', name: userName, username: userName},
    };

    const payloadJson = JSON.stringify(payload);

    const encodedPayload = percentEncodeSpaces
        ? encodeURIComponent(payloadJson).replace(
              /[!'()*]/g,
              (character) => `%${character.charCodeAt(0).toString(16).toUpperCase()}`
          )
        : new URLSearchParams({payload: payloadJson}).toString().slice('payload='.length);

    const rawBody = `payload=${encodedPayload}`;

    const signature = `v0=${createHmac('sha256', signingSecret).update(`v0:${timestamp}:${rawBody}`).digest('hex')}`;

    return request.post(`${SERVER_URL}/slack/interactivity`, {
        data: rawBody,
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Slack-Request-Timestamp': timestamp,
            ...(withSignature ? {'X-Slack-Signature': signature} : {}),
        },
    });
}

interface TelegramCallbackProps {
    callbackData: string;
    request: APIRequestContext;
    secretToken?: string | null;
    userName?: string;
}

export async function postTelegramCallback({
    callbackData,
    request,
    secretToken = CHANNEL_SECRETS.telegramWebhookSecretToken,
    userName = 'bob',
}: TelegramCallbackProps): Promise<APIResponse> {
    const body = {
        callback_query: {
            data: callbackData,
            from: {id: 7, username: userName},
            id: 'cbq1',
            message: {chat: {id: 12345}, message_id: 55},
        },
        update_id: 1,
    };

    return request.post(`${SERVER_URL}/telegram/interactivity`, {
        data: JSON.stringify(body),
        headers: {
            'Content-Type': 'application/json',
            ...(secretToken !== null ? {'X-Telegram-Bot-Api-Secret-Token': secretToken} : {}),
        },
    });
}

interface DiscordInteractionProps {
    body: Record<string, unknown>;
    request: APIRequestContext;
    tamperSignature?: boolean;
}

/**
 * Posts a Discord interaction signed with Ed25519 over `<timestamp><raw body>`, with the private key whose public
 * half the server is configured with (`bytechef.webhook.discord.public-key`).
 */
export async function postDiscordInteraction({
    body,
    request,
    tamperSignature = false,
}: DiscordInteractionProps): Promise<APIResponse> {
    const privateKeyPem = getDiscordPrivateKeyPem();

    if (!privateKeyPem) {
        throw new Error('BYTECHEF_DISCORD_PRIVATE_KEY_PEM must point at the Discord test private key');
    }

    const rawBody = JSON.stringify(body);
    const timestamp = currentEpochSeconds();

    const signature = sign(null, Buffer.from(`${timestamp}${rawBody}`), createPrivateKey(privateKeyPem)).toString(
        'hex'
    );

    const sentSignature = tamperSignature
        ? `${signature.slice(0, -2)}${signature.endsWith('00') ? '11' : '00'}`
        : signature;

    return request.post(`${SERVER_URL}/discord/interactivity`, {
        data: rawBody,
        headers: {
            'Content-Type': 'application/json',
            'X-Signature-Ed25519': sentSignature,
            'X-Signature-Timestamp': timestamp,
        },
    });
}

export function buildDiscordButtonInteraction(customId: string, userName: string): Record<string, unknown> {
    return {
        application_id: 'app1',
        data: {component_type: 2, custom_id: customId},
        id: 'i1',
        member: {user: {id: '9', username: userName}},
        token: 'itok',
        type: 3,
    };
}

interface WhatsAppButtonProps {
    appSecret?: string;
    buttonId: string;
    contactName: string;
    request: APIRequestContext;
    withSignature?: boolean;
}

export async function postWhatsAppButtonReply({
    appSecret = CHANNEL_SECRETS.whatsAppAppSecret,
    buttonId,
    contactName,
    request,
    withSignature = true,
}: WhatsAppButtonProps): Promise<APIResponse> {
    const phoneNumber = '385911234567';

    const body = {
        entry: [
            {
                changes: [
                    {
                        field: 'messages',
                        value: {
                            contacts: [{profile: {name: contactName}, wa_id: phoneNumber}],
                            messages: [
                                {
                                    from: phoneNumber,
                                    id: 'wamid.1',
                                    interactive: {
                                        button_reply: {id: buttonId, title: 'Approve'},
                                        type: 'button_reply',
                                    },
                                    type: 'interactive',
                                },
                            ],
                        },
                    },
                ],
                id: '1',
            },
        ],
        object: 'whatsapp_business_account',
    };

    const rawBody = JSON.stringify(body);

    const signature = `sha256=${createHmac('sha256', appSecret).update(rawBody).digest('hex')}`;

    return request.post(`${SERVER_URL}/whatsapp/interactivity`, {
        data: rawBody,
        headers: {
            'Content-Type': 'application/json',
            ...(withSignature ? {'X-Hub-Signature-256': signature} : {}),
        },
    });
}

interface MattermostActionProps {
    approved?: boolean;
    request: APIRequestContext;
    token?: string;
    userName: string;
}

export async function postMattermostAction({
    approved = true,
    request,
    token,
    userName,
}: MattermostActionProps): Promise<APIResponse> {
    const body = {
        ...(token ? {context: {approved, resumeId: token}} : {}),
        user_id: 'u1',
        user_name: userName,
    };

    return request.post(`${SERVER_URL}/mattermost/interactivity`, {
        data: JSON.stringify(body),
        headers: {'Content-Type': 'application/json'},
    });
}
