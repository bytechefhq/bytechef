export interface AskUserQuestionOptionI {
    description: string;
    label: string;
}

export interface AskUserQuestionI {
    header: string;
    multiSelect: boolean;
    options: AskUserQuestionOptionI[];
    question: string;
}

export interface AskUserQuestionEventI {
    questions: AskUserQuestionI[];
    resumeUrl?: string;
}

export interface ApprovalRequestEventI {
    expiresAt?: string;
    formDescription?: string;
    formTitle?: string;
    formUrl?: string;
    inputs?: unknown[];
    resumeId: string;
    resumeUrl?: string;
}

const EVENT_STREAM_CONTENT_TYPE = 'text/event-stream';
const JSON_CONTENT_TYPE = 'application/json';

export function formatApprovalRequest(event: ApprovalRequestEventI): string {
    const title = event.formTitle || 'Approval requested';
    const description = event.formDescription ? `\n\n${event.formDescription}` : '';
    const expiry = event.expiresAt ? `\n\nExpires ${new Date(event.expiresAt).toLocaleString()}` : '';
    const link = event.formUrl ? `\n\n[Open the approval form](${event.formUrl})` : '';

    return `**${title}**${description}${expiry}${link}`;
}

export function formatAskUserQuestion(event: AskUserQuestionEventI): string {
    if (!Array.isArray(event.questions) || event.questions.length === 0) {
        return '';
    }

    return event.questions
        .map((question) => {
            const options = Array.isArray(question.options) ? question.options : [];
            const optionLines = options
                .map((option, index) => `  ${index + 1}. **${option.label}** — ${option.description}`)
                .join('\n');

            const header = question.header ?? '';
            const text = question.question ?? '';
            const headerSegment = header ? `**${header}**: ` : '';

            return optionLines ? `${headerSegment}${text}\n${optionLines}` : `${headerSegment}${text}`;
        })
        .join('\n\n');
}

export function getApprovalResumeUrl(
    event: Pick<ApprovalRequestEventI, 'resumeId' | 'resumeUrl'>,
    webhookUrl: string,
    baseUrl: string = globalThis.location?.href ?? ''
): string | null {
    if (typeof event.resumeUrl === 'string' && event.resumeUrl.trim().length > 0) {
        return event.resumeUrl;
    }

    if (typeof event.resumeId !== 'string' || event.resumeId.trim().length === 0) {
        return null;
    }

    let origin: string;

    try {
        origin = new URL(webhookUrl, baseUrl || undefined).origin;
    } catch {
        return null;
    }

    return `${origin}/job/resume/${encodeURIComponent(event.resumeId)}`;
}

export function isEventStreamResponse(response: Response): boolean {
    const contentType = response.headers.get('content-type') ?? '';

    return contentType.includes(EVENT_STREAM_CONTENT_TYPE);
}

export function isAcceptedResumeResponse(response: Response): boolean {
    if (response.status === 204) {
        return true;
    }

    if (response.status !== 200) {
        return false;
    }

    const contentType = response.headers.get('content-type') ?? '';

    return contentType.includes(EVENT_STREAM_CONTENT_TYPE) || contentType.includes(JSON_CONTENT_TYPE);
}
