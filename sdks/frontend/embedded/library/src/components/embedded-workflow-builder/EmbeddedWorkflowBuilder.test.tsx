import {act, render} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import EmbeddedWorkflowBuilder from './EmbeddedWorkflowBuilder';

const fireFromIframe = (data: unknown, origin = 'https://app.example') => {
    act(() => {
        window.dispatchEvent(new MessageEvent('message', {data, origin}));
    });
};

describe('EmbeddedWorkflowBuilder', () => {
    it('answers EMBED_READY with the init params', () => {
        const {container} = render(
            <EmbeddedWorkflowBuilder
                baseUrl="https://app.example"
                connectionDialogAllowed
                jwtToken="jwt-1"
                workflowUuid="uuid-1"
            />
        );
        const iframe = container.querySelector('iframe')!;

        expect(iframe.getAttribute('src')).toBe('https://app.example/workflow-builder.html#/embedded/builder/uuid-1');

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        fireFromIframe({type: 'EMBED_READY'});

        expect(postMessage).toHaveBeenCalledWith(
            {
                params: {
                    connectionDialogAllowed: true,
                    environment: 'PRODUCTION',
                    includeComponents: undefined,
                    jwtToken: 'jwt-1',
                },
                type: 'EMBED_INIT',
            },
            'https://app.example'
        );
    });

    it('re-sends EMBED_INIT when the token changes after the iframe is ready', () => {
        const {container, rerender} = render(
            <EmbeddedWorkflowBuilder
                baseUrl="https://app.example"
                connectionDialogAllowed
                jwtToken="jwt-1"
                workflowUuid="uuid-1"
            />
        );
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireFromIframe({type: 'EMBED_READY'});

        postMessage.mockClear();

        rerender(
            <EmbeddedWorkflowBuilder
                baseUrl="https://app.example"
                connectionDialogAllowed
                jwtToken="jwt-2"
                workflowUuid="uuid-1"
            />
        );

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith(
            expect.objectContaining({params: expect.objectContaining({jwtToken: 'jwt-2'}), type: 'EMBED_INIT'}),
            'https://app.example'
        );
    });

    it('ignores EMBED_READY from another origin and messages without data', () => {
        const {container} = render(
            <EmbeddedWorkflowBuilder
                baseUrl="https://app.example"
                connectionDialogAllowed
                jwtToken="jwt-1"
                workflowUuid="uuid-1"
            />
        );
        const postMessage = vi.fn();

        Object.defineProperty(container.querySelector('iframe')!, 'contentWindow', {value: {postMessage}});

        fireFromIframe({type: 'EMBED_READY'}, 'https://evil.example');
        fireFromIframe(null);

        expect(postMessage).not.toHaveBeenCalled();
    });
});
