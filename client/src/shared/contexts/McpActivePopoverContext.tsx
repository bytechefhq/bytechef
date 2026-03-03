import {ReactNode, createContext, useCallback, useContext, useState} from 'react';

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
