/*
    See lda-top/LICENCE (or http://elda.googlecode.com/hg/LICENCE)
    for the licence for this software.

    (c) Copyright 2011 Epimorphics Limited
    $Id$

    File:        ResultSet.java
    Created by:  Dave Reynolds
    Created on:  31 Jan 2010
*/

package com.atomgraph.core.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * Model hash calculator.
 *
 * The hash is a digest of a canonical serialization rather than a fold of per-triple hashes, because
 * it becomes an entity tag, and an entity tag can reach someone who cannot read what it describes -
 * an agent with write access and no read access. A fold by XOR is LINEAR in the set of triples:
 *
 * <pre>
 *     hash(G + t) = hash(G) XOR hashTriple(t)
 * </pre>
 *
 * and a set gains nothing when you add a member it already has. Such an agent can therefore add a
 * triple, see how far the tag moved, and learn whether it was already there - a membership oracle
 * over content it may not read. A digest ends that: the difference between two hashes says nothing
 * about what was added.
 *
 * The same linearity, over a per-triple mix whose shifts overlap, made collisions easy to construct,
 * which matters for the hash's own job: two graphs sharing an entity tag means a conditional write
 * can be satisfied by a graph that is not the one the client read.
 */
public class ModelUtils
{

    /** Digest algorithm the hash is taken with */
    public static final String DIGEST_ALGORITHM = "SHA-256";

    /**
     * Returns a hash of the model's statements.
     *
     * Order-independent, because the serialized statements are sorted: two readings of one graph
     * hash alike. Blank node labels are not stable across readings, so a graph carrying them does not
     * hash consistently - callers needing a stable value skolemize before writing.
     *
     * @param m the model
     * @return hash value
     */
    public static long hashModel(Model m)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, m, Lang.NTRIPLES);

        String[] lines = out.toString(StandardCharsets.UTF_8).split("\n");
        Arrays.sort(lines);

        StringBuilder sorted = new StringBuilder();
        for (String line : lines)
            if (!line.isBlank()) sorted.append(line.stripTrailing()).append('\n');

        return digest(sorted.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static long hashTriple(Triple t)
    {
        long result = 0;
        Node S = t.getSubject(), P = t.getPredicate(), O = t.getObject();
        if (!S.isBlank()) result = (long) S.hashCode() << 32;
        if (!P.isBlank()) result ^= (long) P.hashCode() << 16;
        if (!O.isBlank()) result ^= (long) O.hashCode();
        return result;
    }

    /**
     * Digests bytes into a long, keeping the first eight bytes of the digest.
     *
     * @param bytes the bytes
     * @return hash value
     */
    public static long digest(byte[] bytes)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM); // per call: MessageDigest is not thread-safe
            byte[] hash = digest.digest(bytes);

            long result = 0;
            for (int i = 0; i < Long.BYTES; i++) result = (result << 8) | (hash[i] & 0xff);

            return result;
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalStateException(DIGEST_ALGORITHM + " is required of every JVM", ex);
        }
    }

}
