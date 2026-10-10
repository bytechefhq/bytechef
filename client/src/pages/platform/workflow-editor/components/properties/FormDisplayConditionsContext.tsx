import {createContext, useContext} from 'react';

/**
 * Evaluated display conditions for FORM mode (a property surface with no workflow node to read them off — a tool
 * config dialog, an MCP tool popover). Undefined while unevaluated or unsupported, which leaves every conditional
 * property visible — see Property.tsx.
 *
 * Carried by context rather than by prop because Property nests itself through several intermediaries (object
 * sub-properties, array items, object entries, dynamic sub-properties), and a prop that any one of them forgets to
 * forward silently ungates everything below it — which is exactly how HTTP Client's `body.*` conditions went unhonoured.
 */
const FormDisplayConditionsContext = createContext<Record<string, boolean> | undefined>(undefined);

export const FormDisplayConditionsProvider = FormDisplayConditionsContext.Provider;

export const useFormDisplayConditionsContext = () => useContext(FormDisplayConditionsContext);
