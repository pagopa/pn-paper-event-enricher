// Importazione dei moduli necessari
const fs = require('fs');
const axios = require('axios');
const path = require('path');
const AdmZip = require('adm-zip');
const getArguments = require('./libs/args');
const { AwsClientsWrapper } = require("./libs/AwsClientWrapper");
const { SafeStorageClient } = require("./libs/SafeStorageClient");

async function main() {
    const safeStorageUrl = process.env.SAFESTORAGE_URL;
    const { paId, fileName, envName } = getArguments();
    console.log('paId =', paId, 'fileName =', fileName, 'envName =', envName);

    const awsClient = new AwsClientsWrapper(envName);
    const safeStorageClient = new SafeStorageClient(safeStorageUrl);


    const tempDir = path.join(__dirname, 'temp');
    if (!fs.existsSync(tempDir)) {
        fs.mkdirSync(tempDir);
    }

    // Legge il file sorgente e lo divide in righe
    const fileContent = await readSourceFile(awsClient, fileName);
    const events = fileContent.trim().split('\n').map(JSON.parse);
    console.log('Reading', events.length, 'events from file')

    // Array per mantenere traccia dei file scaricati
    const downloadedFiles = [];

    for(const event of events) {
        const newImage = event.dynamodb.NewImage;
        const iun = newImage.metadata.M.iun.S;
        console.log("reading iun", iun);
        const notification = await awsClient.getNotification(iun);
        if(notification == null) {
            console.error('Notification with iun ' + iun + ' does not exist');
        }
        if(paId === notification.senderPaId.S) {
            console.log('Processing of IUN:', iun);
            const printedPdf = newImage.printedPdf && newImage.printedPdf.S;
            const formattedFileKey = printedPdf.replace("safestorage://", "");
            const presignedUrl =  await safeStorageClient.getPresignedDownloadUrl(formattedFileKey);
            const fileResponse = await safeStorageClient.downloadFile(presignedUrl);
            const fileName = path.basename(formattedFileKey);

            const filePath = path.join(tempDir, fileName);
            const writer = fs.createWriteStream(filePath);

            fileResponse.data.pipe(writer);

            await new Promise((resolve, reject) => {
                writer.on('finish', resolve);
                writer.on('error', reject);
            });

            downloadedFiles.push(filePath);
        }
    }

    // Creazione dello zip dei file scaricati
    const zip = new AdmZip();
    downloadedFiles.forEach(file => {
        zip.addLocalFile(file);
    });
    const zipPath = path.join(__dirname, new Date().toISOString() + '.zip');
    zip.writeZip(zipPath);

    // Rimuovi i file temporanei
    fs.rmSync(tempDir, { recursive: true });
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

main().then();