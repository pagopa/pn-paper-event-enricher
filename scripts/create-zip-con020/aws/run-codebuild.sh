#! /bin/bash


ENV=$1
PA_ID=$2
S3_KEY_SOURCE=$3
SAFESTORAGE_URL=$4
PROFILE=sso_pn-core-$ENV

BUCKET_SOURCE=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "create-zip-con020-codebuild-$ENV" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"Con020InputBucket\") | .OutputValue" )

BUCKET_DESTINATION=$( aws ${aws_command_base_args} \
    cloudformation describe-stacks \
      --profile "$PROFILE" \
      --stack-name "create-zip-con020-codebuild-$ENV" \
      --output json \
  | jq -r ".Stacks[0].Outputs | .[] | select( .OutputKey==\"Con020OutputBucket\") | .OutputValue" )

aws codebuild start-build \
  --profile "$PROFILE" \
  --project-name CreateZipCon020 \
  --environment-variables-override \
  name=PA_ID,value="$PA_ID",type=PLAINTEXT \
  name=BUCKET_SOURCE,value="$BUCKET_SOURCE",type=PLAINTEXT \
  name=S3_KEY_SOURCE,value="$S3_KEY_SOURCE",type=PLAINTEXT \
  name=BUCKET_DESTINATION,value="$BUCKET_DESTINATION",type=PLAINTEXT \
  name=SAFESTORAGE_URL,value="$SAFESTORAGE_URL",type=PLAINTEXT
