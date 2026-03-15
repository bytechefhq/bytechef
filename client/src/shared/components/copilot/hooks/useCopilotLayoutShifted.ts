import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {useEffect, useState} from 'react';

/**
 * Returns a boolean that becomes true/false in sync with CopilotPanel's
 * internal visibility (which uses a double-RAF delay on open). Use this
 * for layout shifts (margin/padding removal) that must start at the same
 * moment as the panel's width transition to avoid a visible "bump".
 */
const useCopilotLayoutShifted = (): boolean => {
    const [layoutShifted, setLayoutShifted] = useState(false);

    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);

    useEffect(() => {
        let outerRafId: number | undefined;
        let innerRafId: number | undefined;

        if (copilotPanelOpen) {
            outerRafId = requestAnimationFrame(() => {
                innerRafId = requestAnimationFrame(() => {
                    setLayoutShifted(true);
                });
            });
        } else {
            setLayoutShifted(false);
        }

        return () => {
            if (outerRafId !== undefined) {
                cancelAnimationFrame(outerRafId);
            }

            if (innerRafId !== undefined) {
                cancelAnimationFrame(innerRafId);
            }
        };
    }, [copilotPanelOpen]);

    return layoutShifted;
};

export default useCopilotLayoutShifted;
