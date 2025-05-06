import argparse
import getpass
import json
from typing import Optional, Dict

import boto3
from botocore.client import BaseClient


def get_s3_client(profile: Optional[str], endpoint_url: Optional[str], access_key: Optional[str], secret_key: Optional[str]):
    if profile:
        session = boto3.Session(profile_name=profile)
        return session.client("s3")
    elif endpoint_url and access_key and secret_key:
        return boto3.client("s3", endpoint_url=endpoint_url, aws_access_key_id=access_key, aws_secret_access_key=secret_key)
    else:
        raise ValueError("You must provide either a profile or endpoint URL and access/secret key.")

def set_tags(s3, bucket: str, key: str, tags: Dict[str, str]):
    tag_set = [{'Key': k, 'Value': v} for k, v in tags.items()]
    s3.put_object_tagging(Bucket=bucket, Key=key, Tagging={'TagSet': tag_set})

def tag_files_recursive(s3: BaseClient, bucket: str, key: str, tags: Dict[str, str]):
    paginator = s3.get_paginator('list_objects_v2')
    page_iterator = paginator.paginate(Bucket=bucket, Prefix=key)
    page_number = 0
    for page in page_iterator:
        page_number += 1
        objects = [obj['Key'] for obj in page.get('Contents', []) if 'Size' in obj]
        print(f"Page {page_number}, Size: {len(objects)}")
        for key in objects:
            set_tags(s3, bucket, key, tags)

def main():
    parser = argparse.ArgumentParser(
        description="Tag objects in S3 bucket"
    )

    parser.add_argument("--profile", type=str, help="AWS profile name")
    parser.add_argument("--endpoint-url", type=str, help="AWS endpoint URL")
    parser.add_argument("--access-key", type=str, help="AWS access key")
    parser.add_argument("--secret-key", type=str, help="AWS secret key")
    parser.add_argument("--bucket", type=str, required=True, help="S3 bucket name")
    parser.add_argument("--prefix", type=str, required=True, help="prefix to tag objects under. E.g. foo/bar/")
    parser.add_argument("--tagging", type=str, required=True, help="tags in format of flat JSON map. E.g. '{\"key\":\"value\"}'")

    args = parser.parse_args()

    # Ask for endpoint URL, access and secret key if not defined nor profile
    if not args.profile:
        if not args.endpoint_url:
            args.endpoint_url = input("Enter endpoint URL: ")
        if not args.access_key:
            args.access_key = input("Enter access key: ")
        if not args.secret_key:
            args.secret_key = getpass.getpass("Enter secret key: ")

    # Parse tags
    tags = json.loads(args.tagging)

    # Tag objects
    print("Starting tagging...")
    s3 = get_s3_client(profile=args.profile, endpoint_url=args.endpoint_url, access_key=args.access_key, secret_key=args.secret_key)
    tag_files_recursive(s3=s3, bucket=args.bucket, key=args.prefix, tags=tags)
    print("Finished tagging")

if __name__ == "__main__":
    main()
