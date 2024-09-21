#! /bin/bash

PROFILE=$1
echo "Creating stack with profile $PROFILE"
aws cloudformation create-stack --profile "$PROFILE" --stack-name create-zip-con020-codebuild \
    --template-body file://cfn/template.yml --capabilities CAPABILITY_NAMED_IAM