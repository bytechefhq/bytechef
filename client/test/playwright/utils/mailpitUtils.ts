import {type APIRequestContext, expect} from '@playwright/test';

export const MAILPIT_URL = process.env.MAILPIT_URL || 'http://localhost:8025';

export const MAIL_TIMEOUT = 60000;

export interface MailpitMessageSummaryI {
    Created: string;
    ID: string;
    Subject: string;
    To: Array<{Address: string}>;
}

export interface MailpitMessageI extends MailpitMessageSummaryI {
    HTML: string;
    Text: string;
}

export async function isMailpitReachable(request: APIRequestContext): Promise<boolean> {
    try {
        const response = await request.get(`${MAILPIT_URL}/api/v1/info`, {timeout: 5000});

        return response.ok();
    } catch {
        return false;
    }
}

export async function searchMailpitMessages(
    request: APIRequestContext,
    query: string
): Promise<MailpitMessageSummaryI[]> {
    const response = await request.get(`${MAILPIT_URL}/api/v1/search`, {params: {limit: 200, query}});

    expect(response.ok()).toBe(true);

    const searchResult = (await response.json()) as {messages: MailpitMessageSummaryI[]};

    return searchResult.messages;
}

export async function getMailpitMessage(request: APIRequestContext, messageId: string): Promise<MailpitMessageI> {
    const response = await request.get(`${MAILPIT_URL}/api/v1/message/${messageId}`);

    expect(response.ok()).toBe(true);

    return (await response.json()) as MailpitMessageI;
}

/**
 * Waits until exactly `count` messages match the Mailpit search (e.g. `to:"approver-x@example.com"`) and returns
 * the newest one in full.
 */
export async function waitForMailpitMessage(
    request: APIRequestContext,
    query: string,
    {count = 1, timeout = MAIL_TIMEOUT}: {count?: number; timeout?: number} = {}
): Promise<MailpitMessageI> {
    let messages: MailpitMessageSummaryI[] = [];

    await expect(async () => {
        messages = await searchMailpitMessages(request, query);

        expect(messages).toHaveLength(count);
    }).toPass({timeout});

    return getMailpitMessage(request, messages[0].ID);
}

export async function countMailpitMessages(request: APIRequestContext, query: string): Promise<number> {
    const messages = await searchMailpitMessages(request, query);

    return messages.length;
}

export function recipientQuery(address: string): string {
    return `to:"${address}"`;
}

export function subjectQuery(subject: string): string {
    return `subject:"${subject}"`;
}

export function extractLinks(html: string): string[] {
    return [...html.matchAll(/href="([^"]+)"/g)].map((linkMatch) => linkMatch[1].replaceAll('&amp;', '&'));
}
