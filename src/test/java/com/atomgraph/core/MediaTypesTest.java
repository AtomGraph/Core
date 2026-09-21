/*
 * Copyright 2026 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atomgraph.core;

import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The media types a resource offers and accepts per entity class.
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class MediaTypesTest
{

    private MediaTypes mediaTypes;

    @BeforeEach
    public void init()
    {
        mediaTypes = new MediaTypes();
    }

    private static boolean contains(List<MediaType> mediaTypes, Lang lang)
    {
        return mediaTypes.stream().anyMatch(mediaType ->
            mediaType.getType().equals(lang.getContentType().getType()) &&
            mediaType.getSubtype().equals(lang.getContentType().getSubType()));
    }

    private static int indexOf(List<MediaType> mediaTypes, Lang lang)
    {
        for (int i = 0; i < mediaTypes.size(); i++)
        {
            MediaType mediaType = mediaTypes.get(i);
            if (mediaType.getType().equals(lang.getContentType().getType()) &&
                mediaType.getSubtype().equals(lang.getContentType().getSubType())) return i;
        }

        return -1;
    }

    /** A model is triples in both directions; quads would have nowhere to put a graph name. */
    @Test
    public void testModelIsTriplesOnly()
    {
        assertTrue(contains(mediaTypes.getReadable(Model.class), Lang.TURTLE));
        assertTrue(contains(mediaTypes.getWritable(Model.class), Lang.TURTLE));

        assertFalse(contains(mediaTypes.getReadable(Model.class), Lang.NQUADS));
        assertFalse(contains(mediaTypes.getWritable(Model.class), Lang.NQUADS));
    }

    /**
     * A dataset is written as quads or, losing its named graphs, as triples — DatasetProvider
     * reduces it to the default graph for a triples language.
     */
    @Test
    public void testDatasetWritesQuadsAndTriples()
    {
        List<MediaType> writable = mediaTypes.getWritable(Dataset.class);

        assertTrue(contains(writable, Lang.NQUADS), "cannot write N-Quads");
        assertTrue(contains(writable, Lang.TRIG), "cannot write TriG");
        assertTrue(contains(writable, Lang.TURTLE), "cannot write Turtle");
        assertTrue(contains(writable, Lang.RDFXML), "cannot write RDF/XML");
    }

    /**
     * Every quad language precedes every triples one. These variants carry no q of their own, so
     * this order decides what a client accepting anything is served — and a lossy representation
     * must never be the default.
     */
    @Test
    public void testDatasetPrefersQuadsOverTriples()
    {
        List<MediaType> writable = mediaTypes.getWritable(Dataset.class);

        int lastQuad = -1, firstTriples = writable.size();
        for (int i = 0; i < writable.size(); i++)
        {
            MediaType mediaType = writable.get(i);
            Lang lang = RDFLanguages.contentTypeToLang(mediaType.getType() + "/" + mediaType.getSubtype());
            if (lang == null) continue;

            if (RDFLanguages.isQuads(lang)) lastQuad = i;
            else if (RDFLanguages.isTriples(lang) && i < firstTriples) firstTriples = i;
        }

        assertTrue(lastQuad < firstTriples,
            "a triples variant precedes a quad one, so a wildcard Accept would lose named graphs");
    }

    /**
     * Reading a dataset stays permissive: a remote endpoint may answer a CONSTRUCT with triples
     * whatever the Accept asked for, and triples read losslessly into the default graph.
     */
    @Test
    public void testDatasetReadsQuadsOnlyByNegotiation()
    {
        List<MediaType> readable = mediaTypes.getReadable(Dataset.class);

        assertTrue(contains(readable, Lang.NQUADS), "cannot read N-Quads");
        assertTrue(contains(readable, Lang.TRIG), "cannot read TriG");
        // the Accept header stays quad-only so a compliant server never returns a lossy format;
        // DatasetProvider still parses triples if one arrives anyway
        assertFalse(contains(readable, Lang.TURTLE), "Turtle is offered in Accept, inviting a lossy response");
    }

    @Test
    public void testNQuadsIsPreferredForReading()
    {
        List<MediaType> readable = mediaTypes.getReadable(Dataset.class);

        assertTrue(indexOf(readable, Lang.NQUADS) >= 0);
        assertTrue(readable.get(indexOf(readable, Lang.NQUADS)).getParameters().containsKey("q"),
            "N-Quads carries no q value, so it cannot be prioritised");
    }

}
