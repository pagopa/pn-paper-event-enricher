const fs = require('fs');
const { computeSHA256, uploadToSafeStorage } = require('./safestorage.js');
const { createCSVFromBOL, writeToCSV, duplicateCSVRows } = require('./csv.js');
const { processBatches } = require('./sqs.js');

async function loadTestCon020() {
    const bolFile = "attachment_example_3pdf.bol"; //TODO: nel test di carico bol-7000.bol
    const binFile = "attachment_example_completed_3pdf.zip"; //TODO: nel test di carico zip da 2,5gb
    const inputCSV = "input-test.csv";
    const outputCSV = "duplicated_" + inputCSV;
    const fileKeys = [];
    const fileNumber = 1;
    const batchSize = 1; //TODO: nel test di carico 2000

    const sha256 = await computeSHA256(binFile);
    const archiveDetails = await createCSVFromBOL(bolFile);
    await writeToCSV(archiveDetails, inputCSV);
    await uploadToSafeStorage(binFile, fileKeys, sha256, fileNumber);
    await duplicateCSVRows(inputCSV, outputCSV, fileNumber);
    await processBatches(outputCSV, fileKeys, sha256, batchSize);

    fs.unlinkSync(outputCSV);
    fs.unlinkSync(inputCSV);

    console.log(`File ${outputCSV} has been deleted.`);
}

loadTestCon020().catch(console.error);
