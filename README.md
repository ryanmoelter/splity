# Splity – a sync engine for couples using YNAB

[![Build badge](https://github.com/ryanmoelter/splity/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ryanmoelter/splity/actions/workflows/build.yml)

(Work in progress)

## Getting started

This assumes that both people's budgets are in the same account, but that they use separate budgets.

### To run:

Either use one of the pre-packaged releases here on github, or package the app using `./gradlew assemble`, and find the zipped file `build/distributions/splity-<version>`. From there, unzip and find the executable in the `splity-<version>/bin/` directory.

(You could also run it with `gradle run` if you don't want to spend the time to package it.)

Add your YNAB token and account names to a `config.yaml` file in the directory in which you run the app, like so:

```yaml
ynabToken: "<ynab token>"
startDate: "<optional inclusive start date, e.g. '2021-02-23' (useful if someone does a fresh start)>"

firstAccount:
  budgetName: "<First person's budget name, e.g. '2020.1'>"
  accountName: "<First person's split account, e.g. 'Split - Sarah'>"
secondAccount:
  budgetName: "<Second person's budget name, e.g. 'Sarah's budget 2020'>"
  accountName: "<Second person's split account, e.g. 'Split - Ryan'>"
```

### Sentry integration

If you (like me) want to be alerted when budget syncing fails, create a new project on [Sentry](https://sentry.io) and put the DSN in your `config.yaml` file.

```yaml
sentryConfig:
  dsn: "<Paste dsn here>"
```
