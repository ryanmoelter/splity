# Splity – a sync engine for couples using YNAB
(Work in progress)

## Getting started
This assumes that both people's budgets are in the same account, but that they use separate budgets.

To run:

Package the app using `./gradlew assemble`, unzip `build/distributions/<version>`, and find the executable in the `build/distributions/<version>/bin/` directory.

Add your YNAB token and account names to a `config.yaml` file in the directory in which you run the app, like so:
```yaml
ynabToken: "<ynab token>"

firstAccount:
  budgetName: "<First person's budget name>"
  accountName: "<First person's split account, e.g. 'Split - Sarah'>"
secondAccount:
  budgetName: "<Second person's budget name>"
  accountName: "<Second person's split account, e.g. 'Split - Ryan'>"
```
