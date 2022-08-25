package minimalJava;

/**
 * Whether to generate index.scip (JSON) or index.scip-protobuf (Protobuf).
 *
 * <p>The Protobuf format is experimental and currently only exists as a proof-of-concept.
 *
 * NOTE: Taken from https://github.com/sourcegraph/scip-java/blob/main/scip-semanticdb/src/main/java/com/sourcegraph/scip_semanticdb/ScipOutputFormat.java
 */
public enum ScipOutputFormat {
  GRAPH_NDJSON,
  GRAPH_PROTOBUF,
  TYPED_PROTOBUF,
  TYPED_NDJSON,
  UNKNOWN;

  public boolean isTyped() {
    return this == TYPED_NDJSON || this == TYPED_PROTOBUF;
  }

  public boolean isNewlineDelimitedJSON() {
    return this == GRAPH_NDJSON || this == TYPED_NDJSON;
  }

  public static ScipOutputFormat fromFilename(String name) {
    if (name.endsWith(".lsif")) return GRAPH_NDJSON;
    if (name.endsWith(".lsif-protobuf")) return GRAPH_PROTOBUF;
    if (name.endsWith(".scip")) return TYPED_PROTOBUF;
    if (name.endsWith(".scip.ndjson")) return TYPED_NDJSON;
    return UNKNOWN;
  }
}
