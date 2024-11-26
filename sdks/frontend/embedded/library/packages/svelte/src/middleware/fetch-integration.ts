import { Integration } from "./model/Integration";
const fetchIntegration = async (): Promise<Integration> => {
  return await fetch('http://localhost:9555/api/embedded/v1/frontend/integrations/1053', {
    headers: {
      'Authorization': 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT2s4M1pVWmtXRlZCWmpCSVJGRXpOMlE1Y0dOQmVGVnlNVEpOU1dzdlNIUjAifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.cBek7AcrbY_OiZPSzbswHHhriBYnV2h30E-n7uRqYcT8pD_LNOo-0fdhLkHalg9oyQrxmWDODFVE1b29Olo5nf3-xtb3Y3PFIIkyVMAKFT-XgH02_yb6oLcVhOceJe01rQ_MUX7-WjvPtvZZ2nvaVlrouq6ow1I-ToJXMinLkquzf6WYONUj-vjqRAGJH243jjFK0mOO_c_jlOSQxJicKAYiQ6R_ltNvZidcKjR3gjDrei04w_hLpiEwHE6rMKot6H2b4FaIILEEePCLG70BrO9CF-7xrnp4-2fkn3RSiMV_4zIFCUbX-0ITOepy8X2WER7cbzWNlMSTtM6hkFqA4A',
      'x-environment': 'test'
    },
    method: 'GET'
  }).then(response => response.json());
};
export default fetchIntegration