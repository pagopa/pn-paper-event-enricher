const fs = require('fs');
const { SQSClient, SendMessageCommand, GetQueueUrlCommand } = require('@aws-sdk/client-sqs');
const { fromIni } = require('@aws-sdk/credential-provider-ini'); 

// Configura le credenziali
const sqsClient = new SQSClient({
    region: 'eu-south-1',
    credentials: fromIni({ profile: 'dev' })
});


const QUEUE_NAME = 'pn-paper-event-enrichment-input';

async function processBatches(outputCSV, fileKeys, sha256, batchSize) {
    const rows = fs.readFileSync(outputCSV, 'utf-8').split('\n').filter(Boolean);

    const commandGetUrl = new GetQueueUrlCommand({ QueueName: QUEUE_NAME });
    const responseGetUrl = await sqsClient.send(commandGetUrl);
    queueUrl = responseGetUrl.QueueUrl;

    for (let i = 0; i < rows.length; i += batchSize) {
        const batch = rows.slice(i, i + batchSize);
        for (let j = 0; j < batch.length; j++) {
            const fileKey = fileKeys[j % fileKeys.length];
            console.log(`Processing row ${batch[j]}`);
            await processRow(fileKey, batch[j], sha256, queueUrl);
        }
        console.log(`Processed ${batchSize} rows`);
        console.log("Waiting 1 minute...");
        await new Promise(resolve => setTimeout(resolve, 60000)); //TODO: test di carico 5 min
    }
}

async function processRow(fileKey, row, sha256, queueUrl) {
    const [requestId, registeredLetterCode, prodType, dateTime] = parseLine(row);
    const messageBody = JSON.stringify({
        analogMail: {
            requestId,
            registeredLetterCode,
            productType: prodType,
            iun: null,
            statusCode: "CON020",
            statusDescription: "Affido conservato",
            statusDateTime: dateTime,
            attachments: [{
                id: "0",
                documentType: "Affido conservato",
                uri: fileKey,
                sha256,
                date: dateTime
            }],
            clientRequestTimeStamp: dateTime
        },
        clientId: "pn-cons-000",
        eventTimestamp: dateTime
    }); //TODO: CONTROLLARE IL MESSAGE BODY

    try {
        console.log(`Sending message to SQS queue: ${messageBody}`);

        const command = new SendMessageCommand({
            QueueUrl: queueUrl,
            MessageBody: messageBody
            });
        
        const response = await sqsClient.send(command);
        console.log('Message sent:', response.MessageId);
    } catch (error) {
        console.error('Error sending message:', error);
    }
}

function parseLine(line) {
    const result = line.split(',');
    console.log(result);
    if (result.length < 4) {
        throw new Error("The line does not contain enough fields.");
    }
    return [result[0], result[1], result[2], result[3]];
}

module.exports = { processBatches };