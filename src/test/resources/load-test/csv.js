const csv = require('fast-csv');
const fs = require('fs');

class IndexData {
    constructor(requestId, registeredLetterCode, p7mEntryName) {
        this.requestId = requestId;
        this.registeredLetterCode = registeredLetterCode;
        this.p7mEntryName = p7mEntryName;
    }
}

async function createCSVFromBOL(filePath) {
    const archiveDetails = {};
    const bolString = fs.readFileSync(filePath, 'utf-8');

    bolString.split("\n").forEach(line => {
        if (line.trim()) {
            const cells = line.split("|");
            if (cells.length > 6) {
                const p7mEntryName = cells[0];
                const requestId = cells[3];
                const registeredLetterCode = cells[6];
                if (p7mEntryName.toLowerCase().endsWith('.pdf')) {
                    const indexData = new IndexData(requestId, registeredLetterCode, p7mEntryName);
                    archiveDetails[p7mEntryName] = indexData;
                }
            }
        }
    });

    return archiveDetails;
}

async function writeToCSV(archiveDetails, outputFile) {
    const currentDate = new Date().toISOString();

    const csvStream = csv.format({ headers: false });
    const writableStream = fs.createWriteStream(outputFile);

    csvStream.pipe(writableStream).on('end', () => process.stdout.write('CSV write completed.\n'));

    for (const indexData of Object.values(archiveDetails)) {
        csvStream.write({
            requestId: indexData.requestId,
            registeredLetterCode: indexData.registeredLetterCode,
            prodType: "RS",
            dateTime: currentDate
        });
    }
    csvStream.end();
}

async function duplicateCSVRows(inputCSV, outputCSV, numberOfFiles) {
    const rows = fs.readFileSync(inputCSV, 'utf-8').split('\n').filter(Boolean);
    const duplicatedRows = rows.flatMap(row => Array(numberOfFiles).fill(row));
    fs.writeFileSync(outputCSV, duplicatedRows.join('\n'));
}

module.exports = { createCSVFromBOL, writeToCSV, duplicateCSVRows };
