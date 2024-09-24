#! /bin/bash

ENV=$1
PROFILE=sso_pn-core-$ENV
PN_INFRA=pn-infra-$ENV
PN_DELIVERY_STORAGE=pn-delivery-storage-$ENV

echo "Creating stack with profile $PROFILE"

PROJECT_NAME='pn-paper-event-enricher'
VERSION='1.0.0'
NUMBER='16'
TEMPLATE_BUCKET='https://s3.eu-south-1.amazonaws.com/cd-pipeline-cdartifactbucket-1r4oljdml2zji/pn-infra/c86c5233fab6b64862dcf7eb8f5c9447590b6f41/runtime-infra'

ALARM_ARN=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_INFRA" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"AlarmSNSTopicArn\") | .OutputValue" )

ALARM_TOPIC_NAME=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_INFRA" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"AlarmSNSTopicName\") | .OutputValue" )

aws cloudformation deploy --profile "$PROFILE" --stack-name pn-paper-event-enricher-storage-"$ENV" \
    --template-file cfn/storage.yml \
    --parameter-overrides \
      ProjectName="$PROJECT_NAME" \
      MicroserviceNumber="$NUMBER" \
      TemplateBucketBaseUrl="$TEMPLATE_BUCKET" \
      Version="$VERSION" \
      AlarmSNSTopicArn="$ALARM_ARN" \
      AlarmSNSTopicName="$ALARM_TOPIC_NAME" \
    --capabilities CAPABILITY_NAMED_IAM
