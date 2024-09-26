const {S3Client} = require("@aws-sdk/client-s3");
const {DynamoDBClient} = require("@aws-sdk/client-dynamodb");
const axios = require("axios");

class SafeStorageClient {

    constructor(safeStorageUrl) {
        this._safeStorageUrl = safeStorageUrl;
    }


    async getPresignedDownloadUrl(fileKey){
        const response = await fetch(`${this._safeStorageUrl}/safe-storage/v1/files/${fileKey}`, {
            method: 'GET',
            headers: {
                'x-pagopa-safestorage-cx-id': 'pn-paper-event-enricher'
            }
        })

        const data = await response.json();
        console.log("response SafeStorage: ", data);
        return data.download.url;
    }

    async downloadFile(presignedUrl) {
        return await axios.get(presignedUrl, {responseType: 'stream'});
    }
}

exports.SafeStorageClient = SafeStorageClient;