export type VoiceActivityType = 'idle' | 'reconnecting' | {tool: string};

export interface VoiceActivityStateI {
    activity: VoiceActivityType;
    endReason: string | null;
}

type ListenerType = () => void;

class VoiceActivityStore {
    private state: VoiceActivityStateI = {activity: 'idle', endReason: null};
    private generation = 0;
    private readonly listeners = new Set<ListenerType>();

    getState = (): VoiceActivityStateI => this.state;

    setState = (partial: Partial<VoiceActivityStateI>): void => {
        this.state = {...this.state, ...partial};

        for (const listener of this.listeners) {
            listener();
        }
    };

    subscribe = (listener: ListenerType): (() => void) => {
        this.listeners.add(listener);

        return () => {
            this.listeners.delete(listener);
        };
    };

    beginSession = (): number => {
        this.generation += 1;

        this.setState({activity: 'idle', endReason: null});

        return this.generation;
    };

    reset = (): void => this.setState({activity: 'idle', endReason: null});

    setActivity = (activity: VoiceActivityType, generation?: number): void => {
        if (generation !== undefined && generation !== this.generation) {
            return;
        }

        this.setState({activity});
    };

    setEndReason = (endReason: string | null, generation?: number): void => {
        if (generation !== undefined && generation !== this.generation) {
            return;
        }

        this.setState({endReason});
    };
}

export const voiceActivityStore = new VoiceActivityStore();
