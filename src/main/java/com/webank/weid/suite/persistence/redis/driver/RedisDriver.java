

package com.webank.weid.suite.persistence.redis.driver;

import com.webank.weid.blockchain.protocol.base.CptBaseInfo;
import com.webank.weid.blockchain.protocol.base.WeIdDocument;
import com.webank.weid.blockchain.protocol.base.WeIdDocumentMetadata;
import com.webank.weid.constant.DataDriverConstant;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.exception.WeIdBaseException;
import com.webank.weid.protocol.request.TransactionArgs;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.suite.persistence.*;
import com.webank.weid.suite.persistence.redis.RedisDomain;
import com.webank.weid.suite.persistence.redis.RedisExecutor;
import com.webank.weid.suite.persistence.redis.RedissonConfig;
import com.webank.weid.util.DataToolUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * redis Driver.
 *
 * @author karenli
 */
public class RedisDriver implements Persistence {

    private static final Logger logger = LoggerFactory.getLogger(RedisDriver.class);

    private static final Integer FAILED_STATUS = DataDriverConstant.REDISSON_EXECUTE_FAILED_STATUS;

    private static final ErrorCode KEY_INVALID = ErrorCode.PRESISTENCE_DATA_KEY_INVALID;

    RedissonConfig redissonConfig = new RedissonConfig();

    RedissonClient client = redissonConfig.redismodelRecognition();

    @Override
    public ResponseData<Integer> add(String domain, String id, String data) {

        if (StringUtils.isEmpty(id)) {
            logger.error("[redis->add] the id of the data is empty.");
            return new ResponseData<>(FAILED_STATUS, KEY_INVALID);
        }
        String dataKey = DataToolUtils.hash(id);
        try {
            RedisDomain redisDomain = new RedisDomain(domain);
            Date date = new Date();
            Object[] datas = {data, date, date};
            return new RedisExecutor(redisDomain).execute(client, dataKey, datas);
        } catch (WeIdBaseException e) {
            logger.error("[redis->add] add the data error.", e);
            return new ResponseData<Integer>(FAILED_STATUS, e.getErrorCode());
        }
    }

    @Override
    public ResponseData<Integer> batchAdd(String domain, Map<String, String> keyValueList) {

        try {
            List<Object> idHashList = new ArrayList<>();
            List<Object> dataList = new ArrayList<>();
            Iterator<String> iterator = keyValueList.keySet().iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                String data = keyValueList.get(id);
                if (StringUtils.isEmpty(id)) {
                    logger.error("[redis->batchAdd] the id of the data is empty.");
                    return new ResponseData<Integer>(FAILED_STATUS, KEY_INVALID);
                }
                idHashList.add(DataToolUtils.hash(id));
                dataList.add(data);
            }
            RedisDomain redisDomain = new RedisDomain(domain);
            List<List<Object>> dataLists = new ArrayList<List<Object>>();
            dataLists.add(idHashList);
            dataLists.add(Arrays.asList(dataList.toArray()));
            //处理失效时间
            dataLists.add(fixedListWithDefault(idHashList.size(), redisDomain.getExpire()));
            //处理创建时间和更新时间
            List<Object> nowList = fixedListWithDefault(idHashList.size(), redisDomain.getNow());
            dataLists.add(nowList);
            dataLists.add(nowList);
            return new RedisExecutor(redisDomain).batchAdd(dataLists, client);
        } catch (WeIdBaseException e) {
            logger.error("[redis->batchAdd] batchAdd the data error.", e);
            return new ResponseData<Integer>(FAILED_STATUS, e.getErrorCode());
        }
    }

    private List<Object> fixedListWithDefault(int size, Object obj) {

        Object[] dates = new Object[size];
        Arrays.fill(dates, obj);
        List<Object> list = new ArrayList<>();
        list.addAll(Arrays.asList(dates));
        return list;
    }

    @Override
    public ResponseData<String> get(String domain, String id) {

        if (StringUtils.isEmpty(id)) {
            logger.error("[redis->get] the id of the data is empty.");
            return new ResponseData<String>(StringUtils.EMPTY, KEY_INVALID);
        }
        //dataKey:id的hash值
        String dataKey = DataToolUtils.hash(id);
        try {
            ResponseData<String> result = new ResponseData<String>();
            //设置result初始值为空字符串
            result.setResult(StringUtils.EMPTY);
            RedisDomain redisDomain = new RedisDomain(domain);
            ResponseData<String> response = new RedisExecutor(redisDomain)
                    .executeQuery(redisDomain.getTableDomain(), dataKey, client);

            if (response.getErrorCode().intValue() == ErrorCode.SUCCESS.getCode()
                    && response.getResult() != null) {
                DefaultValue data = DataToolUtils.deserialize(
                        response.getResult(), DefaultValue.class);
                //超过超时时间，log输出data超时
                if (data != null && data.getExpire() != null
                        && data.getExpire().before(new Date())) {
                    logger.error("[redis->get] the data is expire.");
                    //输出empty以及超过超时时间错误代码
                    return new ResponseData<String>(StringUtils.EMPTY,
                            ErrorCode.PERSISTENCE_DATA_EXPIRE);
                }
                if (data != null && StringUtils.isNotBlank(data.getData())) {
                    result.setResult(
                            new String(
                                    data.getData().getBytes(
                                            DataDriverConstant.STANDARDCHARSETS_ISO),
                                    DataDriverConstant.STANDARDCHARSETS_UTF_8
                            )
                    );
                }
            }
            result.setErrorCode(ErrorCode.getTypeByErrorCode(response.getErrorCode()));
            System.out.println("result=" + result.getResult());
            System.out.println(result.getResult() == null);
            return result;
        } catch (WeIdBaseException e) {
            logger.error("[redis->get] get the data error.", e);
            return new ResponseData<String>(StringUtils.EMPTY, e.getErrorCode());
        }
    }

    @Override
    public ResponseData<Integer> delete(String domain, String id) {

        if (StringUtils.isEmpty(id)) {
            logger.error("[redis->delete] the id of the data is empty.");
            return new ResponseData<Integer>(FAILED_STATUS, KEY_INVALID);
        }
        String dataKey = DataToolUtils.hash(id);
        try {
            RedisDomain redisDomain = new RedisDomain(domain);
            return new RedisExecutor(redisDomain).executeDelete(dataKey, client);
        } catch (WeIdBaseException e) {
            logger.error("[redis->delete] delete the data error.", e);
            return new ResponseData<Integer>(FAILED_STATUS, e.getErrorCode());
        }
    }

    @Override
    public ResponseData<Integer> update(String domain, String id, String data) {

        if (StringUtils.isEmpty(id) || StringUtils.isBlank(this.get(domain, id).getResult())) {
            logger.error("[redis->update] the id of the data is empty.");
            return new ResponseData<Integer>(FAILED_STATUS, KEY_INVALID);
        }
        String dataKey = DataToolUtils.hash(id);
        Date date = new Date();
        try {
            RedisDomain redisDomain = new RedisDomain(domain);
            Object[] datas = {data, date};
            return new RedisExecutor(redisDomain).execute(client, dataKey, datas);
        } catch (WeIdBaseException e) {
            logger.error("[redis->update] update the data error.", e);
            return new ResponseData<Integer>(FAILED_STATUS, e.getErrorCode());
        }
    }

    @Override
    public ResponseData<Integer> addOrUpdate(String domain, String id, String data) {

        ResponseData<String> getRes = this.get(domain, id);
        //如果查询数据存在，或者失效 则进行更新 否则进行新增
        if ((StringUtils.isNotBlank(getRes.getResult())
                && getRes.getErrorCode().intValue() == ErrorCode.SUCCESS.getCode())
                ||
                getRes.getErrorCode().intValue() == ErrorCode.PERSISTENCE_DATA_EXPIRE.getCode()) {
            return this.update(domain, id, data);
        }
        return this.add(domain, id, data);
    }

    @Override
    public ResponseData<Integer> addTransaction(TransactionArgs transactionArgs) {

        if (StringUtils.isEmpty(transactionArgs.getRequestId())) {
            logger.error("[redis->add] the id of the data is empty.");
            return new ResponseData<Integer>(FAILED_STATUS, KEY_INVALID);
        }
        try {
            RedisDomain redisDomain = new RedisDomain(
                    DataDriverConstant.DOMAIN_OFFLINE_TRANSACTION_INFO);
            String datakey = transactionArgs.getRequestId();
            Object[] datas = {
                    transactionArgs.getRequestId(),
                    transactionArgs.getMethod(),
                    transactionArgs.getArgs(),
                    transactionArgs.getTimeStamp(),
                    transactionArgs.getExtra(),
                    transactionArgs.getBatch()
            };
            return new RedisExecutor(redisDomain).execute(client, datakey, datas);
        } catch (WeIdBaseException e) {
            logger.error("[redis->add] add the data error.", e);
            return new ResponseData<Integer>(FAILED_STATUS, e.getErrorCode());
        }
    }

    /*
    以下方法暂不需要，本地部署不需要使用redis方式，默认使用Mysql
     */

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addWeId(String domain, String weId, String documentSchema) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateWeId(String domain, String weId, String documentSchema) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<WeIdDocument> getWeIdDocument(String domain, String weId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<WeIdDocumentMetadata> getMeta(String domain, String weId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> deactivateWeId(String domain, String weId, Boolean state) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<List<String>> getWeIdList(String domain, Integer first, Integer last) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getWeIdCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<CptValue> getCpt(String domain, int cptId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<CptBaseInfo> addCpt(String domain, int cptId, String publisher, String description, String cptSchema, String cptSignature) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<PolicyValue> getPolicy(String domain, int policyId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addPolicy(String domain, int policyId, String publisher, String description, String cptSchema, String cptSignature) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<PresentationValue> getPresentation(String domain, int presentationId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addPresentation(String domain, int presentationId, String creator, String policies) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateCpt(String domain, int cptId, int cptVersion, String publisher, String description, String cptSchema, String cptSignature) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateCredentialTemplate(String domain, int cptId, String credentialPublicKey, String credentialProof) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateCptClaimPolicies(String domain, int cptId, String policies) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<List<Integer>> getCptIdList(String domain, Integer first, Integer last) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getCptCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<List<Integer>> getPolicyIdList(String domain, Integer first, Integer last) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getPolicyCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addAuthorityIssuer(String domain, String weId, String name, String desc, String accValue, String extraStr, String extraInt) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> removeAuthorityIssuer(String domain, String weId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<AuthorityIssuerInfo> getAuthorityIssuerByWeId(String domain, String weId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<AuthorityIssuerInfo> getAuthorityIssuerByName(String domain, String name) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateAuthorityIssuer(String domain, String weId, Integer recognize) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getAuthorityIssuerCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getRecognizedIssuerCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addRole(String domain, String weId, Integer roleValue) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<RoleValue> getRole(String domain, String weId) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateRole(String domain, String weId, Integer roleValue) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addSpecificType(String domain, String typeName, String owner) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<SpecificTypeValue> getSpecificType(String domain, String typeName) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> removeSpecificType(String domain, String typeName) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> updateSpecificTypeFellow(String domain, String typeName, String fellow) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> getIssuerTypeCount(String domain) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<List<String>> getIssuerTypeList(String domain, Integer first, Integer last) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addEvidenceByHash(String domain, String hashValue, String signer, String signature, String log, String updated, String revoked, String extraKey, String group_id) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<EvidenceValue> getEvidenceByHash(String domain, String hash) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<Integer> addSignatureAndLogs(String domain, String hashValue, String signer, String signature, String log, String updated, String revoked, String extraKey) {
        return null;
    }

    @Override
    public com.webank.weid.blockchain.protocol.response.ResponseData<EvidenceValue> getEvidenceByExtraKey(String domain, String extraKey) {
        return null;
    }
}
