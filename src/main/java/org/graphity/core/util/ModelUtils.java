/*
    See lda-top/LICENCE (or http://elda.googlecode.com/hg/LICENCE)
    for the licence for this software.
    
    (c) Copyright 2011 Epimorphics Limited
    $Id$

    File:        ResultSet.java
    Created by:  Dave Reynolds
    Created on:  31 Jan 2010
*/

package org.graphity.core.util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class ModelUtils
{

    public static long hashModel( Model m ) {
    	long result = 0;
    	ExtendedIterator<Triple> it = m.getGraph().find(Node.ANY, Node.ANY, Node.ANY);
		while (it.hasNext()) result ^= hashTriple( it.next() );
    	return result;
	}

	public static long hashTriple(Triple t) {
		long result = 0;
		Node S = t.getSubject(), P = t.getPredicate(), O = t.getObject();
		if (!S.isBlank()) result = (long) S.hashCode() << 32;
		if (!P.isBlank()) result ^= (long) P.hashCode() << 16;
		if (!O.isBlank()) result ^= (long) O.hashCode();
		return result;
	}

}