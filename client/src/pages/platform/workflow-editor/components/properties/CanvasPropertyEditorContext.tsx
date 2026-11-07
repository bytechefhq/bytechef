import {createContext, useContext} from 'react';

const CanvasPropertyEditorContext = createContext(false);

export const CanvasPropertyEditorProvider = CanvasPropertyEditorContext.Provider;

export const useCanvasPropertyEditorContext = () => useContext(CanvasPropertyEditorContext);
