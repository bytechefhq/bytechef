import {describe, expect, it, vi} from 'vitest';
import {act, render} from '@testing-library/react';
import AutomationHub from './AutomationHub';

describe('AutomationHub', () => {
    it('renders the hub iframe and answers EMBED_READY with EMBED_INIT params', () => {
        const {container} = render(
            <AutomationHub
                baseUrl="https://app.example"
                className="h-96"
                environment="STAGING"
                jwtToken="jwt-1"
                tabs={{connections: false}}
                theme={{primaryColor: '#123456'}}
            />
        );
        const iframe = container.querySelector('iframe')!;

        expect(iframe.getAttribute('src')).toBe('https://app.example/automation-hub.html#/embedded/hub');
        expect(container.firstElementChild).toHaveClass('h-96');

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://app.example'})
            );
        });

        expect(postMessage).toHaveBeenCalledWith(
            {
                params: {
                    connectionDialogAllowed: true,
                    defaultLayout: 'grid',
                    editWorkflowAllowed: true,
                    environment: 'STAGING',
                    includeComponents: undefined,
                    jwtToken: 'jwt-1',
                    layoutSwitcherAllowed: true,
                    tabs: {connections: false},
                    theme: {primaryColor: '#123456'},
                },
                type: 'EMBED_INIT',
            },
            'https://app.example'
        );
    });

    it('passes the catalog layout and Edit workflow settings through to the iframe', () => {
        const {container} = render(
            <AutomationHub
                baseUrl="https://app.example"
                defaultLayout="list"
                editWorkflowAllowed={false}
                jwtToken="jwt-1"
                layoutSwitcherAllowed={false}
            />
        );
        const iframe = container.querySelector('iframe')!;

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://app.example'})
            );
        });

        expect(postMessage).toHaveBeenCalledWith(
            expect.objectContaining({
                params: expect.objectContaining({
                    defaultLayout: 'list',
                    editWorkflowAllowed: false,
                    layoutSwitcherAllowed: false,
                }),
            }),
            'https://app.example'
        );
    });

    it('ignores EMBED_READY from another origin', () => {
        const {container} = render(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" />);
        const iframe = container.querySelector('iframe')!;

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://evil.example'})
            );
        });

        expect(postMessage).not.toHaveBeenCalled();
    });

    it('re-sends EMBED_INIT to the same origin when the token changes after the iframe is ready', () => {
        const {container, rerender} = render(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" />);
        const iframe = container.querySelector('iframe')!;

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://app.example'})
            );
        });

        postMessage.mockClear();

        rerender(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-2" />);

        expect(postMessage).toHaveBeenCalledTimes(1);
        expect(postMessage).toHaveBeenCalledWith(
            expect.objectContaining({params: expect.objectContaining({jwtToken: 'jwt-2'}), type: 'EMBED_INIT'}),
            'https://app.example'
        );
    });

    it('does not re-send EMBED_INIT when a rerender passes equal params', () => {
        const {container, rerender} = render(
            <AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" theme={{primaryColor: '#123456'}} />
        );
        const iframe = container.querySelector('iframe')!;

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        act(() => {
            window.dispatchEvent(
                new MessageEvent('message', {data: {type: 'EMBED_READY'}, origin: 'https://app.example'})
            );
        });

        postMessage.mockClear();

        rerender(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" theme={{primaryColor: '#123456'}} />);

        expect(postMessage).not.toHaveBeenCalled();
    });

    it('waits for EMBED_READY before sending a changed token', () => {
        const {container, rerender} = render(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" />);
        const iframe = container.querySelector('iframe')!;

        const postMessage = vi.fn();

        Object.defineProperty(iframe, 'contentWindow', {value: {postMessage}});

        rerender(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-2" />);

        expect(postMessage).not.toHaveBeenCalled();
    });

    it('ignores a message without data', () => {
        render(<AutomationHub baseUrl="https://app.example" jwtToken="jwt-1" />);

        expect(() =>
            act(() => {
                window.dispatchEvent(new MessageEvent('message', {data: null, origin: 'https://app.example'}));
            })
        ).not.toThrow();
    });
});
