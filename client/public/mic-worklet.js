class Pcm16DownsamplerProcessor extends AudioWorkletProcessor {
    constructor(options) {
        super();

        const params = (options && options.processorOptions) || {};

        this.targetSampleRate = params.targetSampleRate || 16000;
        this.frameMs = params.frameMs || 20;
        this.frameSize = Math.max(1, Math.floor((this.targetSampleRate * this.frameMs) / 1000));
        this.acc = [];
    }

    process(inputs) {
        const input = inputs[0];

        if (!input || input.length === 0) {
            return true;
        }

        const channel = input[0];

        if (!channel || channel.length === 0) {
            return true;
        }

        const ratio = sampleRate / this.targetSampleRate;
        const out = new Int16Array(Math.floor(channel.length / ratio));

        for (let i = 0; i < out.length; i++) {
            const sourceIndex = Math.floor(i * ratio);
            const sample = Math.max(-1, Math.min(1, channel[sourceIndex] || 0));

            out[i] = sample < 0 ? sample * 0x8000 : sample * 0x7fff;
        }

        for (let i = 0; i < out.length; i++) {
            this.acc.push(out[i]);
        }

        while (this.acc.length >= this.frameSize) {
            const frame = new Int16Array(this.frameSize);

            for (let i = 0; i < this.frameSize; i++) {
                frame[i] = this.acc[i];
            }

            this.acc.splice(0, this.frameSize);

            this.port.postMessage(frame.buffer, [frame.buffer]);
        }

        return true;
    }
}

registerProcessor('pcm16-downsampler', Pcm16DownsamplerProcessor);
