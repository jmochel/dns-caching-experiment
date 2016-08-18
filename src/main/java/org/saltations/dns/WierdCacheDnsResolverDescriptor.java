package org.saltations.dns;

import sun.net.spi.nameservice.NameService;
import sun.net.spi.nameservice.NameServiceDescriptor;

public class WierdCacheDnsResolverDescriptor implements NameServiceDescriptor {

    @Override
    public NameService createNameService() throws Exception {
        return new WierdCacheDnsResolver();
    }

    @Override
    public String getProviderName() {
        return "WierdCacheDNS";
    }

    @Override
    public String getType() {
        return "dns";
    }

}
