const fs = require('fs');
const path = require('path');
const getArguments = require('./libs/args');
const { AwsClientsWrapper } = require("./libs/AwsClientWrapper");
const { SafeStorageClient } = require("./libs/SafeStorageClient");
const { FileUtil } = require("./libs/FileUtil");

async function main() {
    const { paIdArg, fileName, envName } = getArguments();
    const safeStorageUrl = process.env.SAFESTORAGE_URL;
    const bucketDestination = process.env.BUCKET_DESTINATION;
    const paId = process.env.PA_ID ? process.env.PA_ID : paIdArg;
    console.log('paId =', paId, 'fileName =', fileName, 'envName =', envName);

    const awsClient = new AwsClientsWrapper(envName);
    const safeStorageClient = new SafeStorageClient(safeStorageUrl);
    const fileUtil = new FileUtil;

    const tempDir = path.join(__dirname, 'temp');
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir);
    }

    // Legge il file sorgente e lo divide in righe
    const fileContent = await readSourceFile(awsClient, fileName);

    // Suddividi il file in righe e parsa ciascuna riga
    const events = fileContent.trim().split('\n').map(line => {
        const parsedLine = JSON.parse(line);
        return JSON.parse(parsedLine.Body);
    });
    console.log('Reading', events.length, 'events from file')

    // Array per mantenere traccia dei file scaricati
    const downloadedFiles = [];
    const csvData = [];

    for(const event of events) {
        const iun = event.iun;
        console.log("reading iun", iun);
        const notification = await awsClient.getNotification(iun);
        if(notification == null) {
            console.error('Notification with iun ' + iun + ' does not exist');
        }
        if(paId === notification.senderPaId.S) {
            console.log('Processing of IUN:', iun);
            const recIndex = event.recIndex;
            const sendRequestId = editSendRequestIdWithSendPrefix(event.sendRequestId);
            const generationTime = event.generationTime;
            const eventTime = event.eventTime;
            const registeredLetterCode = event.registeredLetterCode;
            const printedPdf = event.printedPdf;
            const formattedFileKey = printedPdf.replace("safestorage://", "");
            const presignedUrl =  await safeStorageClient.getPresignedDownloadUrl(formattedFileKey);
            const fileResponse = await safeStorageClient.downloadFile(presignedUrl);
            const filePath = await uploadToTmpDir(fileResponse, formattedFileKey, tempDir);

            downloadedFiles.push(filePath);

            // Aggiungi i dati per il CSV
            csvData.push({
                iun,
                recIndex,
                sendRequestId,
                generationTime,
                eventTime,
                registeredLetterCode,
                printedPdf
            });
        }
    }

    // Caricamento dello zip su S3
    const zipFileName =  'dump_pn-paper-event-enrichment-output_' + paId + '_' + new Date().toISOString() + '.zip';
    const zipPath = path.join(__dirname, zipFileName);
    const zipBuffer = fileUtil.buildZipFile(downloadedFiles, tempDir, zipPath, csvData);
    await awsClient.uploadFileToS3(bucketDestination, zipFileName, zipBuffer, 'application/zip');
    console.log(`ZIP file successfully uploaded to S3:: ${zipFileName}`);

    // Rimuovi i file temporanei
    fs.rmSync(tempDir, { recursive: true });
    fs.unlinkSync(zipPath);
}

async function readSourceFile(awsClient, fileName) {
    const bucketSource = process.env.BUCKET_SOURCE;
    const s3KeySource = process.env.S3_KEY_SOURCE;
    let fileContent;
    if(bucketSource && s3KeySource) {
        console.log('Reading from S3 bucket', bucketSource);
        fileContent = await awsClient.downloadFileFromS3(bucketSource, s3KeySource);
    }
    else if(fileName) {
        console.log('Reading from an example file', fileName);
        fileContent = fs.readFileSync(fileName, 'utf-8');
    }
    else {
        throw 'Missing source bucket and source test file';
    }
    return fileContent;
}

async function uploadToTmpDir(fileResponse, formattedFileKey, tempDir) {
    const fileName = path.basename(formattedFileKey);

    const filePath = path.join(tempDir, fileName);
    const writer = fs.createWriteStream(filePath);

    fileResponse.data.pipe(writer);

    await new Promise((resolve, reject) => {
        writer.on('finish', resolve);
        writer.on('error', reject);
    });

    return filePath;
}

function editSendRequestIdWithSendPrefix(sendRequestId) {
    let sendRequestWithSendPrefix;
    // Modifica il sendRequestId
    if (sendRequestId.startsWith('PREPARE_ANALOG')) {
        sendRequestWithSendPrefix = sendRequestId.replace('PREPARE_ANALOG', 'SEND_ANALOG');
    } else if (sendRequestId.startsWith('PREPARE_SIMPLE')) {
        sendRequestWithSendPrefix = sendRequestId.replace('PREPARE_SIMPLE', 'SEND_SIMPLE');
    }

    // Rimuovi la parte '.PCRETRY_<n>'
    sendRequestWithSendPrefix = sendRequestWithSendPrefix.replace(/\.PCRETRY_\d+$/, '');
    return sendRequestWithSendPrefix;
}

main().then();