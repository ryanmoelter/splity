# Splity – a sync engine for couples using YNAB

[![Build badge](https://github.com/ryanmoelter/splity/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/ryanmoelter/splity/actions/workflows/build.yml)

## Getting started

This assumes that both people's budgets are in the same account, but that they use separate budgets.

### Config

Add your YNAB token and account names to a `config.yaml` file, like so:

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

### Run with Docker

Splity is published as a Docker image (`ryanmoelter/splity`). The image runs once and exits, just like the bare executable, so schedule it with whatever you already use (e.g. cron). I recommend every 10 minutes to avoid getting rate limited.

Put your `config.yaml` in a directory like `/srv/splity` and mount it at `/data` (the container's working directory):

```sh
docker run --rm -v /srv/splity:/data ryanmoelter/splity:latest
```

The database is created on the first run in the directory bound to `/data` (e.g. `/srv/splity/incrementalSync.db`), and is read and written in place. Persist it between runs to allow for incremental sync, saving you lots of API calls, network bandwidth, and runtime.

Example crontab entry running every 10 minutes:

```cron
*/10 * * * * docker run --rm -v /srv/splity:/data ryanmoelter/splity:latest >> /var/log/splity.log 2>&1
```

To build the image yourself, build the Gradle distribution first, then copy it in:

```sh
./gradlew installDist && docker build -t splity .
```

<details>
<summary><h3>Run Splity outside of docker</h3></summary>

Either use one of the pre-packaged releases here on GitHub, or package the app using `./gradlew assemble`, and find the zipped file `build/distributions/splity-<version>`. From there, unzip and find the executable in the `splity-<version>/bin/` directory.

(You could also run it with `gradle run` if you don't want to spend the time to package it.)

Put your config file in the same directory as the executable before running it.

Note that running the bare executable or gradle requires Java 25+.

</details>

### Sentry integration

If you (like me) want to be alerted when budget syncing fails, create a new project on [Sentry](https://sentry.io) and put the DSN in your `config.yaml` file.

```yaml
sentryConfig:
  dsn: "<Paste dsn here>"
```

### Troubleshooting

Please create an issue if you run into any problems!

As a workaround until I fix it, I've found that deleting the incremental sync file (`incrementalSync.db`) solves most problems. It will be recreated on the next run.
