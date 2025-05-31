import { Integration } from "./model/Integration";
const fetchIntegration = async (): Promise<Integration> => {
  return await fetch('http://localhost:9555/api/embedded/v1/integrations/1050', {
    headers: {
      'Authorization': 'Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImNIVmliR2xqT25GWmExWkROWEJKWkRGRFJVcDNWVFpCVlhoek1ucE5WVUl2T0VvMVVtZE4ifQ.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.S2jxvT57Cs3XqfqUq56W3UGjQ6VxjGkV7Z0Lh-Z6Cq1oknW0V5f2hND9-NR-sWepnCIno7hdQ5EvZRpNEe-65YjuqlyPWH_li0s3isnPfSLkJH8mmEOySLjKTWV_RgWUDkG3OOrnSNauFucTmMvywByLHBmy_wLJf9pxCDQBY_10fVIOIWykW9hKhjtW0T-DbUDYngfBlOybfmTS8ZU-8PnEEPto4Dl-rt4TifrKQYpKcpjKekrgu4vsEsCGIkG9_bSBtw46LPLiYlnILWVILiOwcZijbrtG5IUf2KmtdaJv2FD_LQILPrQVX23-IDpA76f4ldSzuVe9wMM8JjYAtg',
      'x-environment': 'development'
    },
    method: 'GET'
  }).then(response => response.json());
};
export default fetchIntegration