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

        expect(iframe.getAttribute('src')).toBe('https://app.example/embedded/hub');
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
                    environment: 'STAGING',
                    includeComponents: undefined,
                    jwtToken: 'jwt-1',
                    sharedConnectionIds: [],
                    tabs: {connections: false},
                    theme: {primaryColor: '#123456'},
                },
                type: 'EMBED_INIT',
            },
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
});
