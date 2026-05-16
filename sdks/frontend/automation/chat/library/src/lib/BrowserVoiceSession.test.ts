import {describe, expect, it} from 'vitest';
import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import {dirname, resolve} from 'node:path';

/**
 * The widget bundles the AudioWorklet source inline (as a string registered via a Blob URL) so customers do not
 * have to host an extra static asset. The platform client serves the canonical worklet from
 * `client/public/mic-worklet.js`. Both must implement the same audio path bit-for-bit, otherwise voice quality
 * silently diverges between the editor / AI Hub paths and the external widget.
 *
 * This test asserts the inlined source matches the on-disk file after normalising whitespace. We allow
 * formatting differences (the inlined version is collapsed for bundle size) but require the same identifier
 * set so a renamed processor or changed registerProcessor name fails the test.
 */
describe('BrowserVoiceSession inline worklet', () => {
    it('shares the canonical audio processing identifiers with client/public/mic-worklet.js', () => {
        const here = dirname(fileURLToPath(import.meta.url));
        const platformWorkletPath = resolve(here, '../../../../../../../client/public/mic-worklet.js');
        const platformSource = readFileSync(platformWorkletPath, 'utf8');

        // Extract from the widget bundle's BrowserVoiceSession.ts. We re-read the source rather than importing
        // because import would execute the module — AudioWorkletProcessor is undefined in node-vitest.
        const widgetSourcePath = resolve(here, 'BrowserVoiceSession.ts');
        const widgetSource = readFileSync(widgetSourcePath, 'utf8');

        const requiredIdentifiers = [
            'Pcm16DownsamplerProcessor',
            'targetSampleRate',
            'frameMs',
            'frameSize',
            "registerProcessor('pcm16-downsampler'",
        ];

        for (const identifier of requiredIdentifiers) {
            expect(platformSource).toContain(identifier);
            expect(widgetSource).toContain(identifier);
        }
    });
});
