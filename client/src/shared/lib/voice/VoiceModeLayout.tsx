import {useVoiceControls, useVoiceState} from '@assistant-ui/react';
import {useEffect, useMemo, useRef, useState} from 'react';

import {useVoiceActivityStore} from './useVoiceActivityStore';

const END_REASON_DISPLAY_MS = 5000;

interface VoiceModeLayoutPropsI {
    activeLabel?: string;
    idleLabel?: string;
    sessionLimitSeconds: number;
    speakingLabel?: string;
}

function formatDuration(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

function VoiceOrb({className, isActive, isSpeaking}: {className?: string; isActive: boolean; isSpeaking: boolean}) {
    const ringColor = !isActive ? 'bg-gray-600' : isSpeaking ? 'bg-emerald-400' : 'bg-emerald-600';

    return (
        <div
            className={`relative flex items-center justify-center rounded-full ${className ?? ''}`}
            data-testid="voice-orb"
        >
            <div className={`size-32 rounded-full transition-all duration-300 ${ringColor} opacity-20`} />

            <div className={`absolute size-24 rounded-full transition-all duration-300 ${ringColor} opacity-40`} />

            <div className={`absolute size-16 rounded-full transition-all duration-300 ${ringColor}`} />
        </div>
    );
}

function VoiceConnectButton() {
    const controls = useVoiceControls();

    return (
        <button
            className="rounded-full bg-emerald-600 px-8 py-3 font-medium text-white transition-colors hover:bg-emerald-500"
            onClick={controls.connect}
            type="button"
        >
            Connect
        </button>
    );
}

function VoiceDisconnectButton() {
    const controls = useVoiceControls();

    return (
        <button
            className="rounded-full bg-red-600 px-8 py-3 font-medium text-white transition-colors hover:bg-red-500"
            onClick={controls.disconnect}
            type="button"
        >
            Disconnect
        </button>
    );
}

export function VoiceModeLayout({
    activeLabel = 'Listening',
    idleLabel = 'Tap to start',
    sessionLimitSeconds,
    speakingLabel = 'Speaking',
}: VoiceModeLayoutPropsI) {
    const [remainingSeconds, setRemainingSeconds] = useState(sessionLimitSeconds);
    const [showEndReason, setShowEndReason] = useState(false);

    const endReasonTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const activity = useVoiceActivityStore((state) => state.activity);
    const endReason = useVoiceActivityStore((state) => state.endReason);

    const voiceState = useVoiceState();

    const isActive = voiceState?.status.type === 'running';
    const isSpeaking = voiceState?.mode === 'speaking';
    const statusLabel = !isActive ? idleLabel : isSpeaking ? speakingLabel : activeLabel;

    const chipLabel = useMemo(() => {
        if (!isActive || sessionLimitSeconds <= 0) {
            return sessionLimitSeconds > 0 ? `${formatDuration(sessionLimitSeconds)} LIMIT` : null;
        }

        return `${formatDuration(remainingSeconds)} LEFT`;
    }, [isActive, remainingSeconds, sessionLimitSeconds]);

    const activityLabel = useMemo(() => {
        if (typeof activity === 'object' && activity.tool) {
            return `Looking that up… (${activity.tool})`;
        }

        if (activity === 'reconnecting') {
            return 'Reconnecting…';
        }

        if (showEndReason && endReason) {
            return `Ended: ${endReason.replace(/_/g, ' ')}`;
        }

        return null;
    }, [activity, endReason, showEndReason]);

    useEffect(() => {
        if (!isActive) {
            setRemainingSeconds(sessionLimitSeconds);

            return;
        }

        if (sessionLimitSeconds <= 0) {
            return;
        }

        setRemainingSeconds(sessionLimitSeconds);

        const intervalId = setInterval(() => {
            setRemainingSeconds((previous) => {
                if (previous <= 1) {
                    clearInterval(intervalId);

                    return 0;
                }

                return previous - 1;
            });
        }, 1000);

        return () => clearInterval(intervalId);
    }, [isActive, sessionLimitSeconds]);

    useEffect(() => {
        if (!endReason) {
            setShowEndReason(false);

            return;
        }

        setShowEndReason(true);

        endReasonTimeoutRef.current = setTimeout(() => setShowEndReason(false), END_REASON_DISPLAY_MS);

        return () => {
            if (endReasonTimeoutRef.current) {
                clearTimeout(endReasonTimeoutRef.current);

                endReasonTimeoutRef.current = null;
            }
        };
    }, [endReason]);

    return (
        <div className="flex h-full flex-col bg-black text-white">
            <header className="flex justify-end p-4">
                {chipLabel && (
                    <span className="rounded-full border border-emerald-500/40 px-3 py-1 text-xs tracking-widest text-emerald-400">
                        {chipLabel}
                    </span>
                )}
            </header>

            <main className="flex flex-1 flex-col items-center justify-center gap-8">
                <VoiceOrb className="size-48" isActive={isActive} isSpeaking={isSpeaking} />

                <div className="text-center">
                    <p className="text-xs tracking-[0.3em] text-muted-foreground">{statusLabel.toUpperCase()}</p>

                    {activityLabel && <p className="mt-2 text-xs text-emerald-400">{activityLabel}</p>}
                </div>
            </main>

            <footer className="flex justify-center pb-12">
                {isActive ? <VoiceDisconnectButton /> : <VoiceConnectButton />}
            </footer>
        </div>
    );
}
