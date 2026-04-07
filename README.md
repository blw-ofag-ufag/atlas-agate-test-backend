# Agate Test Backend

This project is used to simulate backends that will use or validate agate tokens.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8900/q/dev/>.

## Github Workflow

This project uses the BLW github workflow as described here: https://github.com/blw-ofag-ufag/atlas-code-github-workflows

## Checkstyle

make sure to add the checkstyle plugin and import checkstyle configuration in your IDE to avoid checkstyle errors when pushing code. The checkstyle configuration file is located at
`checkstyle.xml` in the root of the repository.

Make sure to exclude sql scripts in checkstyle settings like this: *.{sql}

## Semantic versioning

Semantic versioning requires

* package.json: specify which pnpm version is used to execute semantic versioning
* .releaserc.json configure the semantic versioning process, e.g. which branches are used for releases, how to determine the next version, etc.