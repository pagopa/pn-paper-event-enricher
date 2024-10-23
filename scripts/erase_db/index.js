import {
  DynamoDBClient,
  DescribeTableCommand,
  BatchWriteItemCommand,
} from '@aws-sdk/client-dynamodb';
import { DynamoDBDocumentClient, ScanCommand } from '@aws-sdk/lib-dynamodb';
import { fromSSO } from '@aws-sdk/credential-provider-sso';

const region = 'eu-south-1';

const tables = [
  'pn-PaperChannelEventEnrichment'
];

// Validate command-line arguments
if (process.argv.length < 3) {
  console.error('\nUsage: node index.js <sso_profile>');
  console.error('\nExample: node index.js "sso_pn-core-dev"');
  process.exit(1);
}

const profileName = process.argv[2];

// Create the DynamoDB client using SSO credentials
const dynamoClient = new DynamoDBClient({
  credentials: fromSSO({ profile: profileName }),
  region,
});
const dynamoDocClient = DynamoDBDocumentClient.from(dynamoClient);

// Retrieve the primary keys of a table
const getPrimaryKeys = async (tableName) => {
  const command = new DescribeTableCommand({ TableName: tableName });
  const response = await dynamoClient.send(command);
  const keySchema = response.Table.KeySchema;
  const keys = {};

  // Extract partition and sort keys (if any)
  keySchema.forEach((key) => {
    if (key.KeyType === 'HASH') {
      keys.partitionKey = key.AttributeName; // Partition key
    } else if (key.KeyType === 'RANGE') {
      keys.sortKey = key.AttributeName; // Sort key (if any)
    }
  });

  return keys;
};

// Delete all items from a table in batches, handling paginated results
const deleteAllItems = async (tableName) => {
  const primaryKeys = await getPrimaryKeys(tableName);
  let totalDeleted = 0;
  let lastEvaluatedKey = undefined;

  // Loop through paginated results until all items are deleted
  do {
    const scanParams = {
      TableName: tableName,
      ExclusiveStartKey: lastEvaluatedKey,
    };

    const scanCommand = new ScanCommand(scanParams);
    const scanResult = await dynamoDocClient.send(scanCommand);
    const items = scanResult.Items || [];

    if (items.length > 0) {
      // Split items into batches of 25 (max batch size for BatchWriteItem)
      const batches = [];
      for (let i = 0; i < items.length; i += 25) {
        batches.push(items.slice(i, i + 25));
      }

      // Process each batch
      for (const batch of batches) {
        const deleteRequests = batch.map((item) => {
          const keyToDelete = {
            [primaryKeys.partitionKey]: { S: item[primaryKeys.partitionKey] },
          };

          if (primaryKeys.sortKey) {
            keyToDelete[primaryKeys.sortKey] = { S: item[primaryKeys.sortKey] };
          }

          return {
            DeleteRequest: {
              Key: keyToDelete,
            },
          };
        });

        const batchParams = {
          RequestItems: {
            [tableName]: deleteRequests,
          },
        };

        const batchWriteCommand = new BatchWriteItemCommand(batchParams);
        await dynamoClient.send(batchWriteCommand);

        totalDeleted += deleteRequests.length;
      }
    }

    lastEvaluatedKey = scanResult.LastEvaluatedKey;
  } while (lastEvaluatedKey);

  console.log(`Items deleted in ${tableName}: ${totalDeleted}`);
};

// Count the remaining items in the table
const countRemainingItems = async (tableName) => {
  const scanParams = {
    TableName: tableName,
    Select: 'COUNT',
  };

  const scanCommand = new ScanCommand(scanParams);
  const scanResult = await dynamoDocClient.send(scanCommand);
  const count = scanResult.Count;

  console.log(`Remaining items in ${tableName}: ${count}`);
};

const main = async () => {
  for (const table of tables) {
    await deleteAllItems(table);
    await countRemainingItems(table);
  }
};

main();
