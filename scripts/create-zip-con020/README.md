# Create ZIP CON020

## Table of contents
- [Cosa fa lo script](#cosa-fa-lo-script)
- [Formati e convenzioni](#formati-e-convenzioni)
- [Creazione dello stack CloudFormation](#creazione-dello-stack-cloudformation)
- [Esecuzione dello script tramite CodeBuild](#esecuzione-dello-script-tramite-codebuild)

## Cosa fa lo script JS
Lo script JS effettua le seguenti operazioni:
1. Prende in input i seguenti parametri:
    - `PA_ID`: la paId su cui filtrare le sendRequestId del dump, in modo tale che vengano elaborate solo quelle che passano il filtro.
    - `BUCKET_SOURCE`: il bucket di input che ospita i file dump della coda pn-paper-event-enrichment-output, che avrà il nome `create-zip-con020-codebuild-dev-con020-input-bucket-{ENV}`.
    - `BUCKET_DESTINATION`: il bucket di output dove viene salvato lo ZIP, che avrà il nome `create-zip-con020-codebuild-dev-con020-output-bucket-{ENV}`
    - `S3_KEY_SOURCE`: il nome del file DUMP in input.
    - `SAFESTORAGE_URL`: l'URL di Safe Storage per chiedere una presigned url, utile al download dei PDF.
2. Legge il DUMP della coda `pn-paper-event-enrichment-output`, dal bucket dato in input, e per ogni riga:
    - Esegue una getItem sulla tabella `pn-Notifications` usando lo IUN presente nel campo `iun` della riga corrente del DUMP.
    - Confronta il paId del risultato della getItem con quello fornito in input. Se sono diversi, la riga finisce di essere elaborata e viene presa quella successiva.
    - Viene chiamata la getFile di SafeStorage per recuperare una presigned URL ed effettuare il download del PDF con fileKey che si trova nel campo `printedPdf` della riga del DUMP.
    - Viene memorizzato il PDF su una cartella temporanea.
3. Quando tutte le righe del DUMP sono state elaborate, viene creato uno ZIP contenente tutti i PDF della cartella temporanea e un CSV che fa da indice.
4. Viene effettuato il download dello ZIP sul bucket di output.
5. Viene cancellata la cartella temporanea.

## Formati e convenzioni
- Il file DUMP contiene dei JSON inline degli eventi della coda pn-paper-event-enrichment-output. Un esempio di formato del DUMP di input può essere trovato qui: [dynamodb-stream-example.txt](src/example/dynamodb-stream-example.txt).
- Il nome dello ZIP di output ha questa convenzione `dump_pn-paper-event-enrichment-output_{paId}_{timepstamp_now_iso}.zip`.
- Il nome del bucket di input ha questa convenzione `create-zip-con020-codebuild-dev-con020-input-bucket-{ENV}`.
- Il nome del bucket di output ha questa convenzione `create-zip-con020-codebuild-dev-con020-output-bucket-{ENV}`.
- Il CSV indice all'interno dello ZIP ha nome `elenco_stampe.csv`, ha il delimitatore `;` ed ha questo formato:
```csv
iun;recIndex;sendRequestId;generationTime;eventTime;registeredLetterCode;printedPdf
```

## Creazione dello stack CloudFormation
Per creare lo stack CloudFormation (operazione fatta in locale):
1. Loggarsi tramite AWS CLI sull'ambiente su cui creare lo stack, ad esempio `aws sso login --profile sso_pn-core-dev`.
2. Posizionarsi col terminale al path `scripts/create-zip-con020/aws/`
3. Lanciare lo script create-stack.sh eseguendo il comando: `./create-stack.sh {env}` ad esempio `./create-stack.sh dev`.

Il CloudFormation crea le seguenti risorse:
1. **Con020InputBucket**: il bucket di input.
2. **Con020OutputBucket**: il bucket di output.
3. **CodeBuildRole**: ruolo necessario al CodeBuild per effettuare diverse operazioni sulle risorse AWS.
4. **Con020CodeBuildProject**: il CodeBuild che esegue lo script JS.

## Esecuzione dello script JS tramite CodeBuild
Per eseguire il CodeBuild (operazione fatta in locale):
1. Loggarsi tramite AWS CLI sull'ambiente su cui eseguire il CodeBuild, ad esempio `aws sso login --profile sso_pn-core-dev`.
2. Posizionarsi col terminale al path `scripts/create-zip-con020/aws/`
3. Lanciare lo script run-codebuild.sh eseguendo il comando: `./run-codebuild.sh {env} {paId} {dump_filename} {safestorage_url}`
   ad esempio: `./run-codebuild.sh dev paIdExample dynamodb-stream-example.txt http://mock.eu-south-1.vpce.amazonaws.com:8080`

I nomi dei bucket di input e di output vengono recuperati, dallo script run-codebuild.sh, tramite lo stack CloudFormation col seguente comando:
```
BUCKET_SOURCE=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "create-zip-con020-codebuild-$ENV" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"Con020InputBucket\") | .OutputValue" )

BUCKET_DESTINATION=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "create-zip-con020-codebuild-$ENV" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"Con020OutputBucket\") | .OutputValue" )
```

Se si volessero utilizzare altri bucket, modificare lo script run-codebuild.sh.
