const AdmZip = require('adm-zip');
const path = require('path');
const fs = require('fs');

class FileUtil {

    buildZipFile(downloadedFiles, tempDir, zipPath, csvData) {

        // Creazione del CSV
        const csvFilePath = path.join(tempDir, 'index.csv');
        this.generateCSV(csvData, csvFilePath);

        // Creazione dello zip dei file scaricati + l'indice CSV
        const zip = new AdmZip();

        zip.addLocalFile(csvFilePath);
        downloadedFiles.forEach(file => {
            zip.addLocalFile(file);
        });

        zip.writeZip(zipPath);

        // Caricamento dello zip su S3
        return fs.readFileSync(zipPath);
    }

    // Funzione per generare il CSV
    generateCSV(data, filePath) {
        const header = 'iun;recIndex;sendRequestId;generationTime;eventTime;registeredLetterCode;printedPdf';
        const rows = data.map(row =>
            `${row.iun};${row.recIndex};${row.sendRequestId};${row.generationTime};${row.eventTime};${row.registeredLetterCode};${row.printedPdf}`
        );
        const csvContent = [header, ...rows].join('\n');
        fs.writeFileSync(filePath, csvContent);
    }
}

exports.FileUtil = FileUtil;