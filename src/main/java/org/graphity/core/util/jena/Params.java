/*
 * Copyright (C) 2015 Martynas Jusevičius <martynas@graphity.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graphity.core.util.jena;

import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.Iterator ;
import java.util.List ;
import java.util.Map ;

import java.nio.charset.StandardCharsets ;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/** A collection of parameters for protocol use. */

public class Params
{
    // As seen.
    private List<Pair> paramList = new ArrayList<Pair>() ;
    
    // string -> list -> string
    private Map<String, List<String>> params = new HashMap<String, List<String>>() ;
    
    
    /** Create a Params object */
    
    public Params() { }
    
    /** Create a Params object, initialized from another one.  A copy is made
     * so the initial values of the Params object are as of the time this constructor
     * was called.
     *  
     * @param other
     */
    public Params(Params other)
    {
        merge(other) ;
    }
    
    public void merge(Params other)
    {
        params.putAll(other.params) ;
        paramList.addAll(other.paramList) ;
    }

    
    /** Add a parameter.
     * @param name  Name of the parameter
     * @param value Value - May be null to indicate none - the name still goes.
     */
    
    public void addParam(String name, String value)
    {
        Pair p = new Pair(name, value) ;
        paramList.add(p) ;
        List<String> x = params.get(name) ;
        if ( x == null )
        {
            x = new ArrayList<String>() ;
            params.put(name, x) ;
        }
        x.add(value) ;
    }

    /** Valueless parameter */
    public void addParam(String name) { addParam(name, null) ; }
    
    public boolean containsParam(String name) { return params.containsKey(name) ; }
    
    public String getValue(String name)
    {
        List<String> x = getMV(name) ;
        if ( x == null )
            return null ;
        if ( x.size() != 1 )
            throw new MultiValueException("Multiple value ("+x.size()+" when exactly one requested") ; 
        return x.get(0) ;
    }
    
    public List<String> getValues(String name)
    {
        return getMV(name) ;
    }
        
    public void remove(String name)
    {
        // Absolute record
        for ( Iterator<Pair> iter = paramList.iterator() ; iter.hasNext() ; )
        {
            Pair p = iter.next() ;
            if ( p.getName().equals(name) )
                iter.remove() ;
        }
        // Map
        params.remove(name) ;
    }
    
    /** Exactly as seen */
    public List<Pair> pairs()
    {
        return paramList ;
    }
    
    public int count() { return paramList.size() ; }
    
    /** Get the names of parameters - one ocurrence */ 
    public List<String> names()
    {
        List<String> names = new ArrayList<String>() ;
        for (Pair pair : paramList)
        {
            String s = pair.getName() ;
            if ( names.contains(s) )
                continue ;
            names.add(s) ;
        }
        return names ; 
    }
    
    /**
     * Fix for Jena-884: https://issues.apache.org/jira/browse/JENA-884
     * @return 
     */
    public String httpString()
    {
        return URLEncodedUtils.format(paramList, StandardCharsets.UTF_8) ;
    }
    
    private List<String> getMV(String name)
    {
        return params.get(name) ;
    }

    static class MultiValueException extends RuntimeException
    {
        MultiValueException(String msg) { super(msg) ; }
    }
        
    public static class Pair implements NameValuePair
    { 
        String name ;
        String value ;

        Pair(String name, String value) { setName(name) ; setValue(value) ; }
        public String getName()  { return name ;  }
        public String getValue() { return value ; }

        void setName(String name)   { this.name = name ; }
        void setValue(String value) { this.value = value ; }
        
    }
}
