

package com.webank.weid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.collections4.CollectionUtils;

import com.webank.weid.constant.DataDriverConstant;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.suite.persistence.mysql.SqlDomain;
import com.webank.weid.suite.persistence.mysql.SqlExecutor;

public abstract class MockMysqlDriver {

    /**
     * mock DB for storage data.
     */
    public static final Map<String, Map<String, Object>> mockDbMap = 
        new HashMap<String, Map<String, Object>>();
    
    /**
     * the default method for mock mySqlDriver.
     */
    public static void mockMysqlDriver() {

        new MockUp<SqlExecutor>() {
            
            //SqlExecutor executor;
            SqlDomain sqlDomain; 
            @Mock
            public void $init(Invocation invocation, SqlDomain sqlDomain) {
                //this.executor = invocation.getInvokedInstance();
                //Deencapsulation
                this.sqlDomain = sqlDomain;
            }
            
            @Mock
            public ResponseData<Integer> execute(String sql, Object... data) {
                String tableDomain = sqlDomain.getTableName();
                Map<String, Object> dbMap = mockDbMap.get(tableDomain);
                if (sql.startsWith("insert")) {
                    if (!mockDbMap.containsKey(tableDomain)) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    }
                    if (dbMap.containsKey(data[0].toString())) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    }
                    dbMap.put(data[0].toString(), data[1]);
                } else if (sql.startsWith("delete")) {
                    if (!mockDbMap.containsKey(tableDomain)) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    }
                    if (!dbMap.containsKey(data[0].toString())) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.SUCCESS
                        );
                    }
                    dbMap.remove(data[0].toString());
                } else if (sql.startsWith("update")) {
                    if (!mockDbMap.containsKey(tableDomain)) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    }
                    if (!dbMap.containsKey(data[3].toString())) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.SUCCESS
                        );
                    }
                    dbMap.put(data[3].toString(), data[1]);
                } else if (sql.startsWith("CREATE")) {
                    if (mockDbMap.containsKey(tableDomain)) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    }
                    if (!mockDbMap.containsKey(tableDomain)) {
                        mockDbMap.put(tableDomain, new HashMap<String, Object>());
                    }
                }
                return new ResponseData<Integer>(1, ErrorCode.SUCCESS);
            }

            @Mock
            public ResponseData<Map<String, String>> executeQuery(String sql, Object... data) {
                String tableName = sqlDomain.getTableName();
                Map<String, String> map = new HashMap<String, String>();
                if (mockDbMap.containsKey(tableName)) {
                    if (data != null && data.length > 0) {
                        map.put(DataDriverConstant.SQL_COLUMN_DATA,
                            (String)mockDbMap.get(tableName).get(data[0]));
                        return new ResponseData<Map<String, String>>(map, ErrorCode.SUCCESS);
                    }
                    map.put(DataDriverConstant.SQL_COLUMN_DATA, tableName);
                    return new ResponseData<Map<String, String>>(map, ErrorCode.SUCCESS);
                }
                return new ResponseData<Map<String, String>>(null,
                        ErrorCode.PERSISTENCE_EXECUTE_FAILED);
            }
            
            @Mock
            public ResponseData<Integer> batchAdd(String sql, List<List<Object>> dataList) {
                System.out.println("mock...........");
                String tableName = sqlDomain.getTableName();
                List<Object> values = dataList.get(dataList.size() - 1);
                for (List<Object> list : dataList) {
                    if (CollectionUtils.isEmpty(list) || list.size() != values.size()) {
                        return 
                            new ResponseData<Integer>(
                                DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                                ErrorCode.PERSISTENCE_BATCH_ADD_DATA_MISMATCH
                            );  
                    }
                }
                List<Object> idList = dataList.get(0);
                int saveCount = 0;
                for (int i = 0; i < idList.size(); i++) {
                    if (mockDbMap.get(tableName).containsKey(idList.get(i))) {
                        return new ResponseData<Integer>(
                            DataDriverConstant.SQL_EXECUTE_FAILED_STATUS,
                            ErrorCode.PERSISTENCE_EXECUTE_FAILED
                        );
                    } else {
                        mockDbMap.get(tableName)
                            .put(idList.get(i).toString(), dataList.get(1).get(i).toString()); 
                        saveCount++;
                    }
                }
                return new ResponseData<Integer>(saveCount, ErrorCode.SUCCESS);
            }
            
            @Mock
            public void resolveTableDomain(String checkTableSql, String createTableSql) {
                if (!mockDbMap.containsKey(sqlDomain.getTableName())) {
                    mockDbMap.put(sqlDomain.getTableName(), new HashMap<String, Object>());
                }
            }
        };
    }
}
