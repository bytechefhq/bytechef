import {ReactNode, createContext, useCallback, useContext, useEffect, useRef, useState} from 'react';

interface McpActivePopoverContextI {
    activePopoverId: string | null;
    closePopover: () => void;
    openPopover: (id: string) => void;
}

const McpActivePopoverContext = createContext<McpActivePopoverContextI>({
    activePopoverId: null,
    closePopover: () => {},
    openPopover: () => {},
});

export const McpActivePopoverProvider = ({children}: {children: ReactNode}) => {
    const [activePopoverId, setActivePopoverId] = useState<string | null>(null);

    const closePopover = useCallback(() => setActivePopoverId(null), []);
    const openPopover = useCallback((id: string) => setActivePopoverId(id), []);

    return (
        <McpActivePopoverContext.Provider value={{activePopoverId, closePopover, openPopover}}>
            {children}
        </McpActivePopoverContext.Provider>
    );
};

export const useMcpActivePopover = () => useContext(McpActivePopoverContext);

/**
 * Closes the active popover when the owning component unmounts (e.g. its card collapses), but only if this component is
 * the one currently open. Without this, the active popover id outlives the unmounted owner and the popover reopens when
 * the card is expanded again.
 */
export const useCloseActivePopoverOnUnmount = (isPopoverOpen: boolean) => {
    const {closePopover} = useMcpActivePopover();

    const isPopoverOpenRef = useRef(isPopoverOpen);

    isPopoverOpenRef.current = isPopoverOpen;

    useEffect(
        () => () => {
            if (isPopoverOpenRef.current) {
                closePopover();
            }
        },
        [closePopover]
    );
};
