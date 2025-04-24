# Scripts

Helper scripts for SWIM maintenance tasks.

## Installation

- Requires Python 3
- Install dependencies with `pip install -r requirements.txt`
- Configure custom CA in `~/.aws/config` (if required)
```toml
[default]
ca_bundle = "<...>.pem"
```

## Execute

```sh
# help
python ./set_tags.py --help
# example
python ./set_tags.py --endpoint-url http://localhost:9000 --bucket swim-bucket --prefix test-meta/inProcess/ --tagging '{"SWIM_State": "processed"}'
```
