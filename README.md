# code-with-quarkus

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8900/q/dev/>.

## Semantic versioning
Semantic versioning requires 
* package.json: specify which pnpm version is used to execute semantic versioning
* .releaserc.json configure the semantic versioning process, e.g. which branches are used for releases, how to determine the next version, etc.