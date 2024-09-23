#! /bin/bash

ENV=$1
PROFILE=sso_pn-core-$ENV
PN_INFRA=pn-infra-$ENV
PN_DELIVERY_STORAGE=pn-delivery-storage-$ENV

echo "Creating stack with profile $PROFILE"
VPC_ID=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_INFRA" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"VpcId\") | .OutputValue" )

SUBNETS=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_INFRA" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"VpcEgressSubnetsIds\") | .OutputValue" )

SEC_GROUP_IDS=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_INFRA" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"AlbSecurityGroup\") | .OutputValue" )

NOTIFICATIONS_TABLE_ARN=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "$PN_DELIVERY_STORAGE" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"NotificationsDynamoTableArn\") | .OutputValue" )

aws cloudformation deploy --profile "$PROFILE" --stack-name create-zip-con020-codebuild-"$ENV" \
    --template-file cfn/template.yml \
    --parameter-overrides \
    VpcId="$VPC_ID" \
    Subnets="$SUBNETS" \
    SecurityGroupIds="$SEC_GROUP_IDS" \
    NotificationsDynamoTableArn="$NOTIFICATIONS_TABLE_ARN" \
    Env="$ENV" \
    --capabilities CAPABILITY_NAMED_IAM
