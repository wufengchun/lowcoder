package org.lowcoder.plugin.googlesheets;


import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.ServiceAccountCredentials;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.lowcoder.plugin.googlesheets.model.*;
import org.lowcoder.plugin.googlesheets.queryhandler.GoogleSheetsActionHandler;
import org.lowcoder.plugin.googlesheets.queryhandler.GoogleSheetsActionHandlerFactory;
import org.lowcoder.sdk.exception.PluginException;
import org.lowcoder.sdk.models.DatasourceTestResult;
import org.lowcoder.sdk.models.QueryExecutionResult;
import org.lowcoder.sdk.plugin.common.DatasourceQueryEngine;
import org.lowcoder.sdk.plugin.common.QueryExecutionUtils;
import org.lowcoder.sdk.query.QueryVisitorContext;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lowcoder.plugin.googlesheets.GoogleSheetError.GOOGLESHEETS_EMPTY_QUERY_PARAM;
import static org.lowcoder.plugin.googlesheets.GoogleSheetError.GOOGLESHEETS_REQUEST_ERROR;
import static org.lowcoder.plugin.googlesheets.queryhandler.GoogleSheetsActionHandler.*;
import static org.lowcoder.sdk.util.JsonUtils.fromJson;
import static org.lowcoder.sdk.util.JsonUtils.toJson;

public class GoogleSheetsPlugin extends Plugin {
    public GoogleSheetsPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Slf4j
    @Extension
    public static class GoogleSheetsEngine implements DatasourceQueryEngine<GoogleSheetsDatasourceConfig, Object, GoogleSheetsQueryExecutionContext> {

        private static final Object CONNECTION_OBJECT = new Object();
        private final Scheduler scheduler = QueryExecutionUtils.querySharedScheduler();

        @Nonnull
        @Override
        public GoogleSheetsDatasourceConfig resolveConfig(Map<String, Object> configMap) {
            return GoogleSheetsDatasourceConfig.buildFrom(configMap);
        }

        @Override
        public Set<String> validateConfig(GoogleSheetsDatasourceConfig connectionConfig) {
            Set<String> invalids = new HashSet<>();
            if (StringUtils.isBlank(connectionConfig.getServiceAccount())) {
                invalids.add("GOOGLESHEETS_DATASOURCE_CONFIG_ERROR");
            }
            return invalids;
        }

        @Override
        public Mono<DatasourceTestResult> testConnection(GoogleSheetsDatasourceConfig connectionConfig) {
            return Mono.just(DatasourceTestResult.testSuccess());
        }

        @Override
        public Mono<Object> createConnection(GoogleSheetsDatasourceConfig datasourceConfig) {
            return Mono.just(CONNECTION_OBJECT);
        }

        @Override
        public Mono<Void> destroyConnection(Object o) {
            return Mono.empty();
        }

        private GoogleSheetsActionRequest parseGoogleSheetsActionRequest(String actionType, Map<String, Object> comp) {
            if (actionType.equals(APPEND_DATA)) {
                return GoogleSheetsAppendDataRequest.from(comp);
            }
            if (actionType.equals(UPDATE_DATA)) {
                return GoogleSheetsUpdateDataRequest.from(comp);
            }
            Class<? extends GoogleSheetsActionRequest> requestClass = switch (actionType) {
                case READ_DATA -> GoogleSheetsReadDataRequest.class;
                case UPDATE_DATA -> GoogleSheetsUpdateDataRequest.class;
                case CLEAR_DATA -> GoogleSheetsClearDataRequst.class;
                case DELETE_DATA -> GoogleSheetsDeleteDataRequest.class;
                default -> throw new PluginException(GOOGLESHEETS_EMPTY_QUERY_PARAM, "GOOGLESHEETS_QUERY_PARAM_EMPTY");
            };
            GoogleSheetsActionRequest result = fromJson(toJson(comp), requestClass);
            if (result == null) {
                throw new PluginException(GOOGLESHEETS_EMPTY_QUERY_PARAM, "GOOGLESHEETS_QUERY_PARAM_EMPTY");
            }
            return result;
        }

        @Override
        public GoogleSheetsQueryExecutionContext buildQueryExecutionContext(GoogleSheetsDatasourceConfig datasourceConfig,
                Map<String, Object> queryConfig, Map<String, Object> requestParams, QueryVisitorContext queryVisitorContext) {
            String actionType = MapUtils.getString(queryConfig, "commandType", "");
            Map<String, Object> comp = (Map<String, Object>) queryConfig.get("command");
            Map<String, Object> paramMap = requestParams;
            GoogleSheetsActionRequest googleSheetsActionRequest = parseGoogleSheetsActionRequest(actionType, comp);
            googleSheetsActionRequest.renderParams(paramMap);
            if (googleSheetsActionRequest.hasInvalidData()) {
                throw new PluginException(GOOGLESHEETS_EMPTY_QUERY_PARAM, "GOOGLESHEETS_QUERY_PARAM_EMPTY");
            }
            GoogleSheetsQueryExecutionContext context = new GoogleSheetsQueryExecutionContext();
            context.setActionType(actionType);
            context.setVisitorId(queryVisitorContext.getVisitorId());
            context.setGoogleSheetsActionRequest(googleSheetsActionRequest);
            context.setServiceAccount(datasourceConfig.getServiceAccount());
            ServiceAccountJsonUtils serviceAccountJsonUtils = new ServiceAccountJsonUtils();
            serviceAccountJsonUtils.getData(context.getServiceAccount());
            ServiceAccountCredentials serviceAccountCredentials;
            try {
                serviceAccountCredentials = ServiceAccountCredentials.fromPkcs8(
                        serviceAccountJsonUtils.getClientId(),
                        serviceAccountJsonUtils.getClientEmail(),
                        serviceAccountJsonUtils.getPrivateKeyPkcs8(),
                        serviceAccountJsonUtils.getPrivateKeyId(),
                        SheetsScopes.all());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            context.setServiceAccountCredentials(serviceAccountCredentials);
            return context;
        }

        @Override
        public Mono<QueryExecutionResult> executeQuery(Object o, GoogleSheetsQueryExecutionContext context) {
            String actionType = context.getActionType();
            GoogleSheetsActionHandler googleSheetsActionHandler = GoogleSheetsActionHandlerFactory.getGoogleSheetsActionHandler(actionType);
            return googleSheetsActionHandler.execute(o, context)
                    .onErrorResume(e -> {
                        log.error("google sheet execute error", e);
                        return Mono.just(QueryExecutionResult.error(GOOGLESHEETS_REQUEST_ERROR, "GOOGLESHEETS_REQUEST_ERROR",
                                e.getMessage()));
                    });
        }

    }
}
