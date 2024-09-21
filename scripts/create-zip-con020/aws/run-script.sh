#! /bin/bash

# I parametri vengono passati come argomenti allo script
PROFILE=$1
PA_ID=$1
BUCKET_SOURCE=$2
S3_KEY_SOURCE=$3
BUCKET_DESTINATION=$4
SAFESTORAGE_URL=$5

aws codebuild start-build \
  --profile "$PROFILE" \
  --project-name CreateZipCon020 \
  --environment-variables-override \
  name=PA_ID,value="$PA_ID",type=PLAINTEXT \
  name=BUCKET_SOURCE,value="$BUCKET_SOURCE",type=PLAINTEXT \
  name=S3_KEY_SOURCE,value="$S3_KEY_SOURCE",type=PLAINTEXT \
  name=BUCKET_DESTINATION,value="$BUCKET_DESTINATION",type=PLAINTEXT \
  name=SAFESTORAGE_URL,value="$SAFESTORAGE_URL",type=PLAINTEXT
