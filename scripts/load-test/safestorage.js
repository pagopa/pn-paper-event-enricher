const axios = require('axios');
const fs = require('fs');
const crypto = require('crypto');

const SAFE_STORAGE_URL = 'http://localhost:8888/safe-storage/v1/files';

async function computeSHA256(filePath) {
    const fileBuffer = fs.readFileSync(filePath);
    const hash = crypto.createHash('sha256').update(fileBuffer).digest();
    return hash.toString('base64');
}

async function uploadToSafeStorage(filePath, fileKeys, sha256, fileNumber) {
    for (let i = 0; i < fileNumber; i++) {
        console.log("Start appending file key");
        fileKeys.push(await createFile(filePath, sha256));
    }
}

async function createFile(filePath, sha256) {
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

    const fileBuffer = fs.readFileSync(filePath);
    const uploadHeaders = {
        "Content-Type": "application/octet-stream",
        "x-amz-checksum-sha256": sha256,
        "x-amz-meta-secret": secret
    };
    const uploadResponse = await axios.put(uploadUrl, fileBuffer, { headers: uploadHeaders });

    if (uploadResponse.status === 200) {
        console.log(`ARCHIVE FILE LOADED ON SAFESTORAGE KEY ${key}`);
        return `safestorage://${key}`;
    } else {
        console.log(`Error uploading file: ${uploadResponse.data}`);
    }
}

module.exports = { computeSHA256, uploadToSafeStorage };