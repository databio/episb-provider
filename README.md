# Epigenome switchboard data provider

The data provider serves epigenome data to an `episb-hub`.


## Kibana instructions

Kibana's console: http://52.23.250.217:5601/app/kibana#/dev_tools/console?_g=() 

Example query:
```
GET experiments/_search
{
  "query": {
    "match_all": {}
  }
}
```
