# Script per avviare il test di carico

## Prerequisiti
1. Scaricare da pn-confinfo-dev l'archivio da 2.5 GB PN_EXTERNAL_LEGAL_FACTS-2f1465cb10754f9cb47de16f15d59cff.zip
   che si trova bel bucket di Safe Storage.
2. Spostare l'archivo nella cartella load-test
3. Tunnel per raggiungere Safe Storage in locale

## Installazione
- Node.js (v16 o successiva)
- Installare le dipendenze

  ```
  npm install
  ```
  
## Lancio script
Lanciare il comando:
  ```javascript
  node load-test-con020.js <safestoarge-url> <aws-profile>
  ```

dove:
- safestoarge-url: Endpoint Safe Storage della putFile (opzionale, default: http://localhost:8889/safe-storage/v1/files)
- aws-profile: profilo core su cui effettuare il test di carico (opzionale, default: sso_pn-core-dev)

