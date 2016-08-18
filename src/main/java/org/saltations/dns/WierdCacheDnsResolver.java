package org.saltations.dns;

/**
 * Override the DNS resolution process at the JVM level.
 * <p>
 * The default name service provider is done by putting the following JVM
 * argument on the commandline.
 * {@code -Dsun.net.spi.nameservice.provider.1=dns,WierdCacheDNS}. This will
 * cause the JVM to load this DNS resolver only.
 * <p>
 *
 * @implNote The NameService is intentionally access restricted because Oracle
 *           is reserving the right to change the implementation of the DNS
 *           resolution service. We recognize their right to do so :-) AND we
 *           are still going to use this interface because it's the best way to
 *           override the DNS resolution at the JVM level
 *
 * @author jmochel
 *
 */

import com.google.common.collect.Lists;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Address;
import org.xbill.DNS.Cache;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import sun.net.spi.nameservice.NameService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * Note that the resolver code does not use a logger for now because I am avoiding figuring out how to
 * configure a logger in an SPI.
 */

@SuppressWarnings("restriction")
public class WierdCacheDnsResolver implements NameService {

    /**
     * System DNS resolver (what that looks like internally varies with each OS)
     */

    private SimpleResolver systemResolver;

    /**
     * Quick and dirty cache of already resolved hosts
     */

    private Map<String, InetAddress[]> wierdCache = new HashMap<>();

    /**
     * Primary constructor invoked by the SPI interface loader.
     * <p>
     * <H4>Note
     * <p>
     * What we do here is use the system DNS in the constructor to resolve the
     * DNS server we are going to use for the rest of the resolution.
     */

    public WierdCacheDnsResolver() {

        try {

            this.systemResolver = new SimpleResolver();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("An exception was thrown while configuring our Wierd Cache DNS resolver.",
                    e);
        }

        System.out.println("System resolver is instantiated");
    }

    /**
     * @see sun.net.spi.nameservice.NameService#getHostByAddr(byte[])
     *
     * @implNote We do not check for a Null ip being passed in. You had darn well NOT pass a null ip address in.
     */

    public String getHostByAddr(byte[] ip) throws UnknownHostException {
        return Address.getHostName(InetAddress.getByAddress(ip));
    }

    /**
     * @see sun.net.spi.nameservice.NameService#lookupAllHostAddr(String name)
     *
     * @implNote We do not check for a Null name being passed in. You had darn well NOT pass a null name in.
     */

    @Override
    public InetAddress[] lookupAllHostAddr(String name) throws UnknownHostException {

        InetAddress[] resolved = {};

		/*
		 * Use the System DNS resolver
		 */

        Record[] records;

        try {

            System.out.println("We have been asked to find " + name);

			/*
			 * Now lookup the hostname. We are only looking for the type A
			 * records (i.e. IP address). We specifiy a new cache each lookup specifically
			 * so that we don't actually cache anything.
			 */

            Cache cache = new Cache();
            cache.clearCache();

            Lookup lookup = new Lookup(name, Type.A);
            lookup.setResolver(systemResolver);
            lookup.setCache(cache);
            records = lookup.run();

            int result = lookup.getResult();

			/*
			 * If the System DNS is able to resolve the host we then put it into our cache
			 */

            if (result == Lookup.SUCCESSFUL && records != null && records.length >= 1) {

                ARecord a = (ARecord) records[0];
                InetAddress inet = InetAddress.getByAddress(name, a.getAddress().getAddress());

                resolved = Lists.newArrayList(inet).toArray(new InetAddress[] {});

                wierdCache.put(name,resolved);

                System.out.println("We found " + name + " and have added it to the cache");

            }
        } catch (TextParseException e) {
            throw new IllegalArgumentException("An exception was thrown indicating that the host name we are looking up is BAD. Fix it.",e);
        }
        catch (UnknownHostException e)
        {
			/*
			 * EXCEPTION INTENTIONALLY SWALLOWED
			 *
			 *  The unknown host exception comes back with some cryptic messages and we are
			 *  not attempting to log or resolve them. Instead, we are swallowing the exception
			 *  in the hopes that it will be found in the cache.
			 *
			 *  Production code should at least log this.
			 */
        }

        if ( !wierdCache.containsKey(name) )
        {
            throw new UnknownHostException("We did not resolve host: " + name);
        }

        return wierdCache.get(name);
    }

}
