import {type APIRequestContext, test} from '@playwright/test';

import {getDiscordPrivateKeyPem} from './channelCallbackUtils';
import {MAILPIT_URL, isMailpitReachable} from './mailpitUtils';

export const SLOW_TAG = '@slow';

export const isSlowRunEnabled = process.env.PLAYWRIGHT_SLOW === '1';

export const isMultiTenant = process.env.BYTECHEF_MULTI_TENANT === 'true';

/**
 * Sweep-driven cases (expiry, reminder, escalation, reconciliation) wait for a 15-minute sweep or a one-hour
 * expiry, so they only run when explicitly asked for.
 */
export function skipUnlessSlowRunEnabled(): void {
    test.skip(!isSlowRunEnabled, 'Sweep or one-hour expiry case; set PLAYWRIGHT_SLOW=1 to run it');
}

export async function skipUnlessMailpitIsReachable(request: APIRequestContext): Promise<void> {
    test.skip(!(await isMailpitReachable(request)), `Mailpit is not reachable at ${MAILPIT_URL} (set MAILPIT_URL)`);
}

export function skipUnlessOpenAiKeyIsSet(): void {
    test.skip(!process.env.OPENAI_API_KEY, 'OPENAI_API_KEY is not set; the AI agent cases call gpt-4o-mini');
}

export function skipUnlessMultiTenant(): void {
    test.skip(!isMultiTenant, 'Needs a multi-tenant server; set BYTECHEF_MULTI_TENANT=true');
}

export function skipUnlessDiscordKeyIsSet(): void {
    test.skip(
        !getDiscordPrivateKeyPem(),
        'BYTECHEF_DISCORD_PRIVATE_KEY_PEM must point at the private key matching bytechef.webhook.discord.public-key'
    );
}

export function skipUnlessWhatsAppVerifyTokenIsSet(): void {
    test.skip(
        !process.env.BYTECHEF_WHATSAPP_VERIFY_TOKEN,
        'BYTECHEF_WHATSAPP_VERIFY_TOKEN must match bytechef.webhook.whatsapp.verify-token'
    );
}
