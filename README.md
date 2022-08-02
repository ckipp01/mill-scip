# mill-scip

A [Mill](https://com-lihaoyi.github.io/mill/mill/Intro_to_Mill.html) plugin to
create a [SCIP](https://github.com/sourcegraph/scip/blob/main/scip.proto) index
for your Mill build. Most commonly this is used to enable [precise code
navigation](https://docs.sourcegraph.com/code_intelligence/explanations/precise_code_intelligence)
on [Sourcegraph](https://sourcegraph.com/).

_An example using the [Sourcegraph browser
extension](https://docs.sourcegraph.com/integration/browser_extension) enabling
precise navigation on GitHub._

![2022-08-01 15 40 23](https://user-images.githubusercontent.com/13974112/182163135-57e504b2-7b29-42d6-8588-3da6b71b8bba.gif)

You can read more about SCIP in the [announcement blog
post](https://about.sourcegraph.com/blog/announcing-scip).

## Requirements

- This plugin currently requires **at least Mill 0.10.3**

## Quick Start

This plugin is an [external mill
module](https://com-lihaoyi.github.io/mill/mill/Modules.html#_external_modules)
so you don't need to add anything to your build to use it. The easiest way to
run this against your project is by the below command:

```
mill --import ivy:io.chris-kipp::mill-scip::0.2.0 io.kipp.mill.scip.Scip/generate
```

This command will generate an `index.scip` file for you located in your
`out/io/kipp/mill/scip/Scip/generate.dest/` directory.

You can verify that this worked correctly by using the [scip cli
tool](https://github.com/sourcegraph/scip).

```
❯ scip stats --from out/io/kipp/mill/scip/Scip/generate.dest/index.scip
{
  "documents": 12,
  "linesOfCode": 427,
  "occurrences": 1211,
  "definitions": 285
}
```

## Uploading to Sourcegraph

Currently the easiest way to upload to sourcegraph is by having a workflow that
generates your `index.scip` and then using the [sourcegraph cli
tool](https://docs.sourcegraph.com/cli) to upload it. More than likely there
will be a GitHub action to do this for your Mill builds in the future when this
project is more feature complete.

```yml
name: Sourcegraph
on:
  push:
    branches:
      - main
  pull_request:
  
jobs:
  scip:
    runs-on: ubuntu-latest
    name: "Upload SCIP"
    steps:
      - uses: actions/checkout@v3
      - uses: coursier/cache-action@v6
      - uses: actions/setup-java@v3
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Generate SCIP File
        run: ./mill --import ivy:io.chris-kipp::mill-scip::0.2.0 io.kipp.mill.scip.Scip/generate

      - name: Upload SCIP file
        run: |
          mkdir -p bin
          curl -L https://sourcegraph.com/.api/src-cli/src_linux_amd64 -o bin/src
          chmod +x bin/src
          ./bin/src code-intel upload -trace=3 -root . -file out/io/kipp/mill/scip/Scip/generate.dest/index.scip -github-token $GITHUB_TOKEN
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Limitations

- Currently `mill-scip` works for Scala 2 modules and Scala 3 modules only. Pure
- Java modules aren't yet supported due to some issues when running on Java 17. You
    can track the progress of this
    [here](https://github.com/com-lihaoyi/mill/issues/1983) as it needs to be
    addressed upstream.

## How does this work?

The [manual configuration
guide](https://sourcegraph.github.io/scip-java/docs/manual-configuration.html)
of `scip-java` does a good job at outlining the approach taken here. Part of the
design of this plugin was that it's important that the user doesn't have to
change anything in their build to use it, which isn't the easiest with Mill.
Therefore the following steps outline how we arrive at the `index.scip` file.

- We capture all of the `JavaModule`s in your build
- We hijack all the necessary settings that are necessary to compile your
    project and then add some additional ones.
    - If it's a Scala 2 project we fetch the [semanticdb compiler
        plugin](https://scalameta.org/docs/semanticdb/guide.html#scalac-compiler-plugin)
        and add it to compilation classpath as well as the relevant
        `scalacOptions`.
    - If it's a Scala 3 project enable the production of SemanticDB as it's part
        of the compiler.
- With these new updated settings we do a "compile-like" task which mimics the
    compile task but produces semanticDB.
- Once we have semanticDB we utilize
  [scip-java](https://sourcegraph.github.io/scip-java/) as a library to slurp up
  all the semanticDB files and produce a `index.scip` file.
