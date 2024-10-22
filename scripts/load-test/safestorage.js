const axios = require('axios');
const fs = require('fs');
const crypto = require('crypto');

async function computeSHA256(filePath) {
    return "ea25rM8bfJ82//SSbDUu/QEuNxUsEROpt8WNBtZ2aYc=";
    // const fileBuffer = fs.readFileSync(filePath);
    // const hash = crypto.createHash('sha256').update(fileBuffer).digest();
    // return hash.toString('base64');
}

async function uploadToSafeStorage(filePath, fileKeys, sha256, fileNumber, SAFE_STORAGE_URL) {
    for (let i = 0; i < fileNumber; i++) {
        console.log("Start appending file key");
        fileKeys.push(await createFile(filePath, sha256, SAFE_STORAGE_URL));
    }
}

async function createFile(filePath, sha256, SAFE_STORAGE_URL) {
    const cxId = "pn-test";
    const body = {
        contentType: "application/octet-stream",
        documentType: "PN_EXTERNAL_LEGAL_FACTS",
        status: "SAVED"
    };

    const headers = {
        "x-pagopa-safestorage-cx-id": cxId,
        "x-api-key": "",
        "x-checksum": "SHA-256",
        "x-checksum-value": sha256
    };

    console.log("Start call to safe storage");
    const response = await axios.post(SAFE_STORAGE_URL, body, { headers });
    const { uploadUrl, secret, key } = response.data;
    // const uploadResponse = await axios.put(uploadUrl, fileBuffer, { headers: uploadHeaders });
    const uploadResponse = await uploadToS3PresignedUrl(filePath, uploadUrl, sha256, secret);

    if (uploadResponse.status === 200) {
        console.log(`ARCHIVE FILE LOADED ON SAFESTORAGE KEY ${key}`);
        return `safestorage://${key}`;
    } else {
        console.log(`Error uploading file: ${uploadResponse.data}`);
    }
}

async function uploadToS3PresignedUrl(filePath, uploadUrl, sha256, secret) {
    const fileSize = fs.statSync(filePath).size; // Ottieni la dimensione del file
    const readStream = fs.createReadStream(filePath); // Legge il file a blocchi

    try {
        const uploadResponse = await axios.put(uploadUrl, readStream, {
            headers: {
                'Content-Length': fileSize, // S3 richiede la lunghezza del file
                'Content-Type': 'application/octet-stream', // Imposta il tipo di contenuto corretto
                "x-amz-checksum-sha256": sha256,
                "x-amz-meta-secret": secret
            },
            maxBodyLength: Infinity, // Evita limiti sulla dimensione del corpo della richiesta
            maxContentLength: Infinity // Evita limiti sulla dimensione del contenuto
        });
        console.log('Upload completato con successo:', uploadResponse.status);
        return uploadResponse;
    } catch (error) {
        console.error('Errore durante l\'upload su S3:', error.message);
        return null;
    }
}

module.exports = { computeSHA256, uploadToSafeStorage };