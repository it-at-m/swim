# Scripts

Helper scripts for SWIM maintenance tasks.

## Installation

- Requires Python 3
- Install dependencies with `pip install -r requirements.txt`
- Configure custom CA in `~/.aws/config` (if required)
```
[default]
ca_bundle = ~/root_cert.pem
```
- (Optional) Configure profiles in `~/.aws/credentials` (Ensure security)
```
[local]
aws_access_key_id = minio
aws_secret_access_key = Test1234
endpoint_url = http://localhost:9000
```

## Execute

See the command help for a description of the different arguments.

```sh
# help
python ./set_tags.py --help
# example with arguments (asks for access-key and secret-key if not defined)
python ./set_tags.py --endpoint-url http://localhost:9000 --bucket swim-bucket --prefix test-meta/inProcess/ --tagging '{\"SWIM_State\": \"processed\"}'
# example with profile (endpoint-url, access-key and secret-key need to be defined in profile)
python ./set_tags.py --profile local --bucket swim-bucket --prefix test-meta/inProcess/ --tagging '{\"SWIM_State\": \"processed\"}'
```
