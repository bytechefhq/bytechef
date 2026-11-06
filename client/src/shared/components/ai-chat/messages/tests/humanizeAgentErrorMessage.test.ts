import {describe, expect, it} from 'vitest';

import {humanizeAgentErrorMessage} from '../humanizeAgentErrorMessage';

describe('humanizeAgentErrorMessage', () => {
    it('extracts the message from an Anthropic error envelope', () => {
        const raw =
            'org.springframework.ai.retry.NonTransientAiException: 400 - {"type":"error","error":{"type":"invalid_request_error","message":"Your credit balance is too low."},"request_id":"req_1"}';

        expect(humanizeAgentErrorMessage(raw)).toBe('Your credit balance is too low.');
    });

    it('extracts a top-level message', () => {
        expect(humanizeAgentErrorMessage('HTTP 429 {"message":"Rate limit reached"}')).toBe('Rate limit reached');
    });

    it('prefers the nested error message over the top-level one', () => {
        expect(humanizeAgentErrorMessage('{"error":{"message":"nested"},"message":"top"}')).toBe('nested');
    });

    it('falls back to the top-level message when the nested one is not a string', () => {
        expect(humanizeAgentErrorMessage('{"error":{"message":{"text":"x"}},"message":"top"}')).toBe('top');
    });

    it('strips the exception class when the JSON has no usable message', () => {
        expect(humanizeAgentErrorMessage('java.lang.IllegalStateException: Bad state {"code":1}')).toBe(
            'Bad state {"code":1}'
        );
    });

    it('strips the exception class when the JSON message is empty', () => {
        expect(humanizeAgentErrorMessage('com.example.ProviderError: failed {"message":""}')).toBe(
            'failed {"message":""}'
        );
    });

    it('strips the exception class when the braces do not hold valid JSON', () => {
        expect(humanizeAgentErrorMessage('java.lang.RuntimeException: bad {json')).toBe('bad {json');
        expect(humanizeAgentErrorMessage('java.lang.RuntimeException: a { b } c')).toBe('a { b } c');
    });

    it('strips an inner-class exception prefix and keeps multi-line messages', () => {
        expect(humanizeAgentErrorMessage('com.example.Outer$InnerError: line one\nline two')).toBe(
            'line one\nline two'
        );
    });

    it('returns the input unchanged when nothing matches', () => {
        expect(humanizeAgentErrorMessage('Something went wrong')).toBe('Something went wrong');
        expect(humanizeAgentErrorMessage('closing } before { opening')).toBe('closing } before { opening');
        expect(humanizeAgentErrorMessage('')).toBe('');
    });

    it('does not strip a prefix that is not an exception or error class', () => {
        expect(humanizeAgentErrorMessage('com.example.Warning: careful')).toBe('com.example.Warning: careful');
    });
});
