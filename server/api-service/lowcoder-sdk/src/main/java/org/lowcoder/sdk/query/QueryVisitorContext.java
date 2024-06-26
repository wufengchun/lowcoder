package org.lowcoder.sdk.query;

import lombok.Getter;
import org.lowcoder.sdk.models.Property;
import org.springframework.http.HttpCookie;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Getter
public class QueryVisitorContext {

    private final String visitorId;
    private final String applicationOrgId;
    private final MultiValueMap<String, HttpCookie> cookies;

    private final int systemPort;

    private final Mono<List<Property>> authTokenMono;
    private final Set<String> disallowedHosts;

    public QueryVisitorContext(String visitorId, String applicationOrgId, int systemPort,
            MultiValueMap<String, HttpCookie> cookies, Mono<List<Property>> authTokenMono, Set<String> disallowedHosts) {
        this.visitorId = visitorId;
        this.applicationOrgId = applicationOrgId;
        this.systemPort = systemPort;
        this.cookies = cookies;
        this.authTokenMono = authTokenMono;
        this.disallowedHosts = disallowedHosts;
    }
}
