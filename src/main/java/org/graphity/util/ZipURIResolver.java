/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.graphity.util;

import java.io.IOException;
import java.util.zip.ZipFile;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ZipURIResolver implements URIResolver
{
    private static final Logger log = LoggerFactory.getLogger(ZipURIResolver.class);
    
    private ZipFile zipFile = null;
    
    public ZipURIResolver(ZipFile zipFile)
    {
	this.zipFile = zipFile;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException
    {
	if (log.isDebugEnabled()) log.debug("Resolving href: {} base: {}", href, base);

	try
	{
	    // set system ID?
	    return new StreamSource(zipFile.getInputStream(zipFile.getEntry(href)));
	}
	catch (IOException ex)
	{
	    if (log.isDebugEnabled()) log.debug("Error resolving from ZipFile", ex);
	}
	
	return null;
    }
    
}
