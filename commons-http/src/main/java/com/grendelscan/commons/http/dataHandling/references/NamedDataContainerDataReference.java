/**
 * 
 */
package com.grendelscan.commons.http.dataHandling.references;

import java.util.Arrays;

/**
 * @author david
 * 
 */
public class NamedDataContainerDataReference implements DataReference
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private final byte[] name;

    public NamedDataContainerDataReference(final byte[] name)
    {
        this.name = name;
    }

    @Override
    public NamedDataContainerDataReference clone()
    {
        return new NamedDataContainerDataReference(Arrays.copyOf(name, name.length));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.grendelscan.commons.http.dataHandling.references.DataReference#debugString()
     */
    @Override
    public String debugString()
    {
        return "Container name: " + new String(name);
    }

    public final byte[] getName()
    {
        return name;
    }

}
