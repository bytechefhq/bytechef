import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import AiHubHtmlInteractivePane from '../AiHubHtmlInteractivePane';

const HTML = '<!doctype html><html><head></head><body><h1>Hi</h1></body></html>';

describe('AiHubHtmlInteractivePane', () => {
    it('renders an iframe with sandbox="allow-scripts" and srcDoc', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        const iframe = screen.getByTitle('widget.html') as HTMLIFrameElement;

        expect(iframe).toBeInTheDocument();
        expect(iframe).toHaveAttribute('sandbox', 'allow-scripts');
        expect(iframe.getAttribute('srcdoc') || iframe.getAttribute('srcDoc')).toBe(HTML);
    });

    it('maximizes via the toolbar button and applies the fixed-overlay class', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        const button = screen.getByRole('button', {name: /maximize/i});

        fireEvent.click(button);

        const wrapper = screen.getByTestId('html-pane-wrapper');

        expect(wrapper.className).toMatch(/fixed/);
        expect(wrapper.className).toMatch(/inset-0/);
        expect(wrapper.className).toMatch(/z-50/);
    });

    it('collapses via Escape after maximizing', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        fireEvent.click(screen.getByRole('button', {name: /maximize/i}));

        const wrapper = screen.getByTestId('html-pane-wrapper');

        expect(wrapper.className).toMatch(/fixed/);

        fireEvent.keyDown(document, {key: 'Escape'});

        expect(wrapper.className).not.toMatch(/fixed/);
    });

    it('shows minimize button after maximize', () => {
        render(<AiHubHtmlInteractivePane content={HTML} name="widget.html" />);

        fireEvent.click(screen.getByRole('button', {name: /maximize/i}));

        expect(screen.getByRole('button', {name: /minimize/i})).toBeInTheDocument();
    });
});
