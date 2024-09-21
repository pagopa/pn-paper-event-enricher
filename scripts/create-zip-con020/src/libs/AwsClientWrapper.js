const { fromIni } = require("@aws-sdk/credential-provider-ini");
const { S3Client, GetObjectCommand, PutObjectCommand } = require("@aws-sdk/client-s3");
const {
  DynamoDBClient,
  ScanCommand,
  GetItemCommand,
  PutItemCommand,
  QueryCommand,
  DeleteItemCommand,
} = require("@aws-sdk/client-dynamodb");

function awsClientCfg(env, account) {
  const self = this;
  return {
    region: "eu-south-1",
    credentials: fromIni({
      profile: `sso_pn-${account}-${env}`,
    }),
  };
}

async function sleep(ms) {
  return new Promise((accept, reject) => {
    setTimeout(() => accept(null), ms);
  });
}

class AwsClientsWrapper {
  // FIXME parametri profilo e regione
  constructor(env /* dev, test, uat, prod */) {
    if(env) {
      this._s3Client = new S3Client(awsClientCfg(env, "core"));
      this._dynamoClient = new DynamoDBClient(awsClientCfg(env, "core"));
    }
    else {
      this._s3Client = new S3Client();
      this._dynamoClient = new DynamoDBClient();
    }
  }


  async getNotification(iun) {
    let input = {
      TableName: "pn-Notifications",
      Key: { iun: { S: iun } },
    };

    let response = await this._dynamoClient.send(new GetItemCommand(input));

    return response.Item;
  }


// Funzione per scaricare il file da S3
  async downloadFileFromS3(bucketName, key) {
    const command = new GetObjectCommand({
      Bucket: bucketName,
      Key: key,
    });

    const { Body } = await this._s3Client.send(command);

    // Converti lo stream in stringa
    return await new Promise((resolve, reject) => {
      const chunks = [];
      Body.on('data', chunk => chunks.push(chunk));
      Body.on('error', reject);
      Body.on('end', () => resolve(Buffer.concat(chunks).toString('utf-8')));
    });
  }

  async uploadFileToS3(bucketName, fileName, fileBuffer, contentType) {
    const command = new PutObjectCommand({
      Bucket: bucketName,
      Key: fileName,
      Body: fileBuffer,
      ContentType: contentType
    });

    await this._s3Client.send(command);
  }


}

exports.AwsClientsWrapper = AwsClientsWrapper;
