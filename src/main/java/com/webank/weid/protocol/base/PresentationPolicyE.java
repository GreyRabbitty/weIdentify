

package com.webank.weid.protocol.base;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.constant.CredentialConstant;
import com.webank.weid.protocol.inf.JsonSerializer;
import com.webank.weid.util.DataToolUtils;

/**
 * The base data structure to handle Credential info.
 *
 * @author junqizhang 2019.04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PresentationPolicyE extends Version implements JsonSerializer {
    
    /**
     * the serialVersionUID.
     */
    private static final long serialVersionUID = 3607089314849566766L;

    private static final Logger logger = LoggerFactory.getLogger(PresentationPolicyE.class);

    /**
     * Policy ID.
     */
    private Integer id;

    /**
     * represent who publish this presentation policy.
     */
    private String orgId;

    /**
     * represent who publish this presentation policy.
     */
    private String policyPublisherWeId;

    /**
     * specify which properties in which credential are needed.
     */
    private Map<Integer, ClaimPolicy> policy;

    /**
     * extra data which policy presenter can use it store some specific business data.
     */
    private Map<String, String> extra;

    /**
     * 新增字段，标识是支持零知识证明的policy还是原来的.
     */
    private String policyType = "original";

    /**
     * create the PresentationPolicyE with policyFileName, 
     * please make sure the JSON file in your classPath.
     * 
     * @param policyFileName the policyFileName
     * @return the PresentationPolicyE
     */
    public static PresentationPolicyE create(String policyFileName) {
        PresentationPolicyE policy = null;
        try {
            JsonNode jsonNode = null;
            //获取policyJson文件 转换成JsonNode
            File file = new File(policyFileName);
            logger.info("create policy file path:{}|{}", file.getAbsolutePath(), policyFileName);
            if (file.exists()) {
                jsonNode = DataToolUtils.loadJsonObjectFromFile(file);
            } else {
                // 去除了 / 开头 ("/" + policyFileName)
                jsonNode = DataToolUtils.loadJsonObjectFromResource(policyFileName);
            }

            if (jsonNode == null) {
                logger.error("can not find the {} file in your classpath.", policyFileName);
                return policy;
            }
            policy = fromJson(jsonNode.toString());
        } catch (IOException e) {
            logger.error("create PresentationPolicyE has error, please check the log.", e);
        }
        return policy;
    }
    
    /**
     * create the PresentationPolicyE with JSON String.
     * 
     * @param json the JSON String
     * @return the PresentationPolicyE
     */
    public static PresentationPolicyE fromJson(String json) {
        PresentationPolicyE policy = null;
        try {
            //将Json转换成Map
            HashMap<String, Object> policyMap = 
                DataToolUtils.deserialize(json, HashMap.class);
            //获取policyJson中的policy 转换成Map
            HashMap<Integer, Object> claimMap = 
                (HashMap<Integer, Object>)policyMap.get(CredentialConstant.CLAIM_POLICY_FIELD);
            //遍历claimMap
            Iterator<Integer> it = claimMap.keySet().iterator();
            while (it.hasNext()) {
                //得到每一个claim
                HashMap<String, Object> claim = (HashMap<String, Object>)claimMap.get(it.next());
                //得到fieldsToBeDisclosed转换成Map
                HashMap<String, Object> disclosedMap = 
                    (HashMap<String, Object>)claim.get(
                        CredentialConstant.CLAIM_POLICY_DISCLOSED_FIELD
                    );
                //覆盖原来的fieldsToBeDisclosed为字符串
                claim.put(
                    CredentialConstant.CLAIM_POLICY_DISCLOSED_FIELD,
                    DataToolUtils.serialize(disclosedMap)
                );
            }
            //重新序列化为policyJson
            String value = DataToolUtils.serialize(policyMap);
            //反序列化policyJson为PresentationPolicyE
            return DataToolUtils.deserialize(value, PresentationPolicyE.class);
        } catch (Exception e) {
            logger.error("create PresentationPolicyE has error, please check the log.", e);
        }
        return policy;
    }
    
    @Override
    public String toJson() {
        String jsonString = DataToolUtils.serialize(this);
        HashMap<String, Object> policyEMap = DataToolUtils.deserialize(jsonString, HashMap.class);
        Map<String, Object> policy1 = 
            (HashMap<String, Object>)policyEMap.get(CredentialConstant.CLAIM_POLICY_FIELD);
        for (Map.Entry<String, Object> entry : policy1.entrySet()) {
            HashMap<String, Object> claimPolicyMap = (HashMap<String, Object>)entry.getValue();
            HashMap<String, Object> disclosureMap = 
                DataToolUtils.deserialize(
                    (String)claimPolicyMap.get(CredentialConstant.CLAIM_POLICY_DISCLOSED_FIELD),
                    HashMap.class
                );
            claimPolicyMap.put(CredentialConstant.CLAIM_POLICY_DISCLOSED_FIELD, disclosureMap);
        }
        return DataToolUtils.serialize(policyEMap);
    }

    /**
     * transfer PresentationPolicyE class from weid-blockchain.
     * @param presentation the PresentationPolicyE object in weid-blockchain
     * @return PresentationPolicyE
     */
    public static PresentationPolicyE fromBlockChain(com.webank.weid.blockchain.protocol.base.PresentationPolicyE presentation) {
        PresentationPolicyE presentationPolicyE = new PresentationPolicyE();
        presentationPolicyE.setId(presentation.getId());
        presentationPolicyE.setOrgId(presentation.getOrgId());
        presentationPolicyE.setPolicyPublisherWeId(presentation.getPolicyPublisherWeId());
        presentationPolicyE.setExtra(presentation.getExtra());
        presentationPolicyE.setPolicyType(presentation.getPolicyType());
        Map<Integer, ClaimPolicy> policyMap = new HashMap<>();
        for(Map.Entry<Integer, String> entry  : presentation.getPolicy().entrySet()){
            ClaimPolicy claimPolicy = new ClaimPolicy();
            claimPolicy.setFieldsToBeDisclosed(entry.getValue());
            policyMap.put(entry.getKey(), claimPolicy);
        }
        presentationPolicyE.setPolicy(policyMap);
        return presentationPolicyE;
    }
}
