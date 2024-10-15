echo "### CREATE QUEUES FOR PN-PAPER-EVENT-ENRICHER ###"
queues="pn-paper-event-enrichment-input pn-paper-archives"
for qn in $(echo $queues | tr " " "\n");do
  echo creating queue $qn ...
  aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
      sqs create-queue \
      --attributes '{"DelaySeconds":"2","VisibilityTimeout":"18000"}'\
      --queue-name $qn
done

echo "### CREATE PN PAPER CHANNEL EVENT ENRICHMENT TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-PaperChannelEventEnrichment \
    --attribute-definitions \
        AttributeName=hashKey,AttributeType=S \
        AttributeName=sortKey,AttributeType=S \
    --key-schema \
        AttributeName=hashKey,KeyType=HASH \
        AttributeName=sortKey,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=5

echo "Initialization terminated"