package org.lowcoder.impl.mock;

import jakarta.annotation.Nonnull;
import org.lowcoder.domain.datasource.service.impl.ClientBasedConnectionPool.ClientBasedDatasourceCacheKey;
import org.lowcoder.sdk.models.DatasourceConnectionConfig;
import org.lowcoder.sdk.models.DatasourceTestResult;
import org.lowcoder.sdk.plugin.common.DatasourceConnector;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MockDatasourceConnector implements DatasourceConnector<MockConnection, MockDatasourceConnectionConfig> {

    private static final ConcurrentHashMap<ClientBasedDatasourceCacheKey, AtomicInteger> CREATE_TIMES = new ConcurrentHashMap<>();

    public int getCreateTimes(DatasourceConnectionConfig datasourceConfig) {
        if (datasourceConfig instanceof MockDatasourceConnectionConfig mockDatasourceDetailConfig) {
            ClientBasedDatasourceCacheKey clientBasedDatasourceCacheKey = ClientBasedDatasourceCacheKey.of(mockDatasourceDetailConfig.datasource());
            AtomicInteger atomicInteger = CREATE_TIMES.get(clientBasedDatasourceCacheKey);
            if (atomicInteger != null) {
                return atomicInteger.get();
            }
        }
        return 0;
    }

    @Override
    public Mono<MockConnection> createConnection(MockDatasourceConnectionConfig connectionConfig) {
        ClientBasedDatasourceCacheKey clientBasedDatasourceCacheKey = ClientBasedDatasourceCacheKey.of(connectionConfig.datasource());
        CREATE_TIMES.putIfAbsent(clientBasedDatasourceCacheKey, new AtomicInteger(0));
        CREATE_TIMES.get(clientBasedDatasourceCacheKey).incrementAndGet();
        return Mono.just(new MockConnection());
    }

    @Override
    public Mono<Void> destroyConnection(MockConnection mockConnection) {
        mockConnection.close();
        return Mono.empty();
    }

    @Nonnull
    @Override
    public MockDatasourceConnectionConfig resolveConfig(Map<String, Object> configMap) {
        return new MockDatasourceConnectionConfig(null);
    }

    @Override
    public Set<String> validateConfig(MockDatasourceConnectionConfig connectionConfig) {
        return Collections.emptySet();
    }

    @Override
    public Mono<DatasourceTestResult> testConnection(MockDatasourceConnectionConfig connectionConfig) {
        throw new UnsupportedOperationException();
    }
}
