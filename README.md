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

- This plugin currently only supports the **Mill 0.10.x** series

## Quick Start

This plugin is an [external Mill
module](https://com-lihaoyi.github.io/mill/mill/Modules.html#_external_modules)
so you don't need to add anything to your build to use it.

The quickest way to use this is actually by using the
[`scip-java`](https://sourcegraph.github.io/scip-java/) cli. You can install it
with [Coursier](https://get-coursier.io/docs/cli-installation).

```sh
cs install --contrib scip-java
```

Once installed, you just need to run `scip-java index` in your workspace:

```sh
scip-java index
```

`scip-java` will actually use this plugin to genreate an `index.scip` which you
can then find at the root of your project.

You can also verify that this worked correctly by using the [scip cli
tool](https://github.com/sourcegraph/scip).

_Here is an example after running on the [Courser code base](https://github.com/coursier/coursier)_
```
❯ scip stats
{
  "documents": 450,
  "linesOfCode": 46090,
  "occurrences": 112426,
  "definitions": 18979
}
```

## Uploading to Sourcegraph

More than likely the reason you're generating your `index.scip` is to upload to
Sourcegraph. The easiest way to do this is in a GitHub action workflow like you
see below. For convenience the following curl command will create it for you in
your repo:

```sh
curl -sLo .github/workflows/sourcegraph.yml --create-dirs https://raw.githubusercontent.com/ckipp01/mill-scip/main/.github/workflows/sourcegraph.yml
```

_Example workflow_
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
      - uses: coursier/setup-action@v1
        with:
          jvm: 'temurin:17'
          apps: scip-java

      - name: Generate SCIP File
        run: scip-java index

      - name: Install src
        run: yarn global add @sourcegraph/src

      - name: Upload SCIP file
        run: src code-intel upload -github-token $GITHUB_TOKEN
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## Generating with Mill

You can also use Mill directly to create your index by doing the following:

```sh
mill --import ivy:io.chris-kipp::mill-scip::0.3.0 io.kipp.mill.scip.Scip/generate
```

This command will generate an `index.scip` file for you located in your
`out/io/kipp/mill/scip/Scip/generate.dest/` directory.

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
