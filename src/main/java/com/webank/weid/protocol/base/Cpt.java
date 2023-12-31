

package com.webank.weid.protocol.base;

import java.util.HashMap;
import java.util.Map;

import com.webank.weid.protocol.response.RsvSignature;
import lombok.Data;

/**
 * The base data structure for the CPT.
 *
 * @author junqizhang
 */
@Data
public class Cpt {

    /**
     * Base info of cpt.
     */
    protected CptBaseInfo cptBaseInfo = new CptBaseInfo();

    /**
     * The cpt json schema.
     */
    protected Map<String, Object> cptJsonSchema;

    /**
     * The meta data.
     */
    protected MetaData metaData = new MetaData();

    /**
     * Gets the cpt id.
     *
     * @return the cpt id
     */
    public Integer getCptId() {
        return cptBaseInfo.getCptId();
    }

    /**
     * Sets the cpt id.
     *
     * @param cptId the new cpt id
     */
    public void setCptId(Integer cptId) {
        cptBaseInfo.setCptId(cptId);
    }

    /**
     * Gets the cpt version.
     *
     * @return the cpt version
     */
    public Integer getCptVersion() {
        return cptBaseInfo.getCptVersion();
    }

    /**
     * Sets the cpt version.
     *
     * @param cptVersion the new cpt version
     */
    public void setCptVersion(Integer cptVersion) {
        cptBaseInfo.setCptVersion(cptVersion);
    }

    /**
     * Gets the cpt publisher.
     *
     * @return the cpt publisher
     */
    public String getCptPublisher() {
        return metaData.getCptPublisher();
    }

    /**
     * Sets the cpt publisher.
     *
     * @param cptPublisher the new cpt publisher
     */
    public void setCptPublisher(String cptPublisher) {
        metaData.setCptPublisher(cptPublisher);
    }

    /**
     * Gets the cpt signature.
     *
     * @return the cpt signature
     */
    public String getCptSignature() {
        return metaData.getCptSignature();
    }

    /**
     * Sets the cpt signature.
     *
     * @param cptSignature the new cpt signature
     */
    public void setCptSignature(String cptSignature) {
        metaData.setCptSignature(cptSignature);
    }

    /**
     * Gets the created.
     *
     * @return the created
     */
    public long getCreated() {
        return metaData.getCreated();
    }

    /**
     * Sets the created.
     *
     * @param created the new created
     */
    public void setCreated(long created) {
        metaData.setCreated(created);
    }

    /**
     * Gets the updated.
     *
     * @return the updated
     */
    public long getUpdated() {
        return metaData.getUpdated();
    }

    /**
     * Sets the updated.
     *
     * @param updated the new updated
     */
    public void setUpdated(long updated) {
        metaData.setUpdated(updated);
    }

    /**
     * transfer cpt MetaData from weid-blockchain.
     * @param cptData the cpt MetaData class in weid-blockchain
     * @return RsvSignature
     */
    public static Cpt fromBlockChain(com.webank.weid.blockchain.protocol.base.Cpt cptData) {
        Cpt cpt = new Cpt();
        cpt.setCptBaseInfo(CptBaseInfo.fromBlockChain(cptData.getCptBaseInfo()));
        cpt.setCptJsonSchema(cptData.getCptJsonSchema());
        cpt.setMetaData(MetaData.fromBlockChain(cptData.getMetaData()));
        return cpt;
    }

    /**
     * The base data structure for CPT meta data.
     */
    @Data
    public static class MetaData {

        /**
         * The weIdentity DID of the publisher who register this CPT.
         */
        private String cptPublisher;

        /**
         * The cpt signature for the weIdentity DID and json schema data in Base64.
         */
        private String cptSignature;

        /**
         * The cpt create timestamp.
         */
        private long created;

        /**
         * The cpt update timestamp.
         */
        private long updated;

        /**
         * transfer cpt MetaData from weid-blockchain.
         * @param data the cpt MetaData class in weid-blockchain
         * @return RsvSignature
         */
        public static MetaData fromBlockChain(com.webank.weid.blockchain.protocol.base.Cpt.MetaData data) {
            MetaData metaData = new MetaData();
            metaData.setUpdated(data.getUpdated());
            metaData.setCreated(data.getCreated());
            metaData.setCptSignature(data.getCptSignature());
            metaData.setCptPublisher(data.getCptPublisher());
            return metaData;
        }
    }
}
