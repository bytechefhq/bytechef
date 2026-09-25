import {createContext, useContext} from 'react';

const FormulaEnabledContext = createContext<boolean | undefined>(undefined);

export const FormulaEnabledProvider = FormulaEnabledContext.Provider;

export const useFormulaEnabledContext = () => useContext(FormulaEnabledContext);
