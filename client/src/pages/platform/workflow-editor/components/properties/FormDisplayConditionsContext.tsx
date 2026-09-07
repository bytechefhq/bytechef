import {createContext, useContext} from 'react';

const FormDisplayConditionsContext = createContext<Record<string, boolean> | undefined>(undefined);

export const FormDisplayConditionsProvider = FormDisplayConditionsContext.Provider;

export const useFormDisplayConditionsContext = () => useContext(FormDisplayConditionsContext);
