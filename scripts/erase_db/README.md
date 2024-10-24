# Erase DynamoDB tables

## Tabella dei Contenuti
- [Descrizione](#descrizione)
- [Installazione](#installazione)
- [Utilizzo](#utilizzo)

## Descrizione
Questo script consente di eliminare tutti gli elementi dalle tabelle che gestiscono le gare di pn-paper-channel. L'autenticazione avviene attraverso AWS SSO. Lo script supporta la cancellazione in batch, eliminando fino a 25 elementi per volta.

## Installazione
- Node.js (v16 o successiva)
- Installare le dipendenze

  ```
  npm install
  ```

## Utilizzo
Lo script richiede il profilo sso come argomento:

```
node index.js "sso_pn-core-dev"
```