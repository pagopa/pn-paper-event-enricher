const fs = require('fs');
const { computeSHA256, uploadToSafeStorage } = require('./safestorage.js');
const { createCSVFromBOL, writeToCSV, duplicateCSVRows } = require('./csv.js');
const { processBatches } = require('./sqs.js');

const args = process.argv.slice(2);
const SAFE_STORAGE_URL = args[0] || 'http://localhost:8888/safe-storage/v1/files';
const profile = args[1] || 'dev';

async function loadTestCon020() {
    const bolFile = "bol-7000.bol"; //TODO cambia il nome
    const binFile = "PN_EXTERNAL_LEGAL_FACTS-2f1465cb10754f9cb47de16f15d59cff.zip"; //TODO: nel test di carico zip da 2,5gb
    const inputCSV = "input-test.csv";
    const outputCSV = "duplicated_" + inputCSV;
    const fileKeys = [];
    const fileNumber = 16;
    const batchSize = 2000;

    const sha256 = await computeSHA256(binFile);
    const archiveDetails = await createCSVFromBOL(bolFile);
    await writeToCSV(archiveDetails, inputCSV);
    await uploadToSafeStorage(binFile, fileKeys, sha256, fileNumber, SAFE_STORAGE_URL);
    await duplicateCSVRows(inputCSV, outputCSV, fileNumber);
    await processBatches(outputCSV, fileKeys, sha256, batchSize, profile);

    fs.unlinkSync(outputCSV);
    fs.unlinkSync(inputCSV);

    console.log(`File ${outputCSV} has been deleted.`);
}

loadTestCon020().catch(console.error);
