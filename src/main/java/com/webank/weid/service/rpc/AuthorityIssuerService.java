

package com.webank.weid.service.rpc;

import java.util.List;

import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.base.IssuerType;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.protocol.request.RemoveAuthorityIssuerArgs;
import com.webank.weid.blockchain.protocol.response.ResponseData;

/**
 * Service inf for operations on Authority Issuer.
 *
 * @author chaoxinhu 2018.10
 */
public interface AuthorityIssuerService {

    /**
     * Register a new Authority Issuer on Chain.
     *
     * <p>The input argument actually includes: WeIdentity DID, Name, CreateDate, and Accumulator
     * Value. They will be stored into the 3 fields on the chain: the Bytes32 field (Name); the Int
     * field (create date); the Dynamic Bytes field (accValue). The data Read and Write sequence is
     * fixed in the above mentioned order.
     *
     * @param args the args
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> registerAuthorityIssuer(RegisterAuthorityIssuerArgs args);

    /**
     * Remove a new Authority Issuer on Chain.
     *
     * @param args the args
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> removeAuthorityIssuer(RemoveAuthorityIssuerArgs args);

    /**
     * Check whether the given WeIdentity DID is an authority issuer, or not.
     *
     * @param weId the WeIdentity DID
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> isAuthorityIssuer(String weId);

    /**
     * Recognize this WeID to be an authority issuer.
     *
     * @param weId the WeID
     * @param weIdPrivateKey the private key set
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> recognizeAuthorityIssuer(String weId, WeIdPrivateKey weIdPrivateKey);

    /**
     * De-recognize this WeID to no longer be and authority issuer.
     *
     * @param weId the WeID
     * @param weIdPrivateKey the private key set
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> deRecognizeAuthorityIssuer(String weId, WeIdPrivateKey weIdPrivateKey);

    /**
     * Query the authority issuer information from a given WeIdentity DID.
     *
     * @param weId the WeIdentity DID
     * @return authority issuer info
     */
    ResponseData<AuthorityIssuer> queryAuthorityIssuerInfo(String weId);

    /**
     * Get all of the authority issuer.
     *
     * @param index start position
     * @param num number of returned authority issuer in this request
     * @return Execution result
     */
    ResponseData<List<AuthorityIssuer>> getAllAuthorityIssuerList(Integer index, Integer num);

    /**
     * Register a new issuer type.
     *
     * @param callerAuth the caller
     * @param issuerType the specified issuer type
     * @return Execution result
     */
    ResponseData<Boolean> registerIssuerType(WeIdAuthentication callerAuth, String issuerType);

    /**
     * Marked an issuer as the specified issuer type.
     *
     * @param callerAuth the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param targetIssuerWeId the weId of the issuer who will be marked as a specific issuer type
     * @return Execution result
     */
    ResponseData<Boolean> addIssuerIntoIssuerType(
        WeIdAuthentication callerAuth,
        String issuerType,
        String targetIssuerWeId
    );

    /**
     * Removed an issuer from the specified issuer list.
     *
     * @param callerAuth the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param targetIssuerWeId the weId of the issuer to be removed from a specific issuer list
     * @return Execution result
     */
    ResponseData<Boolean> removeIssuerFromIssuerType(
        WeIdAuthentication callerAuth,
        String issuerType,
        String targetIssuerWeId
    );

    /**
     * Check if the given WeId is belonging to a specific issuer type.
     *
     * @param issuerType the issuer type
     * @param targetIssuerWeId the WeId
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> isSpecificTypeIssuer(
        String issuerType,
        String targetIssuerWeId
    );

    /**
     * Get all specific typed issuer in a list.
     *
     * @param issuerType the issuer type
     * @param index the start position index
     * @param num the number of issuers
     * @return the list
     */
    ResponseData<List<String>> getAllSpecificTypeIssuerList(
        String issuerType,
        Integer index,
        Integer num
    );

    /**
     * Get an issuer's WeID from its name (org ID).
     *
     * @param orgId the org id
     * @return WeID
     */
    ResponseData<String> getWeIdByOrgId(String orgId);
    
    /**
     * get the issuer count.
     * @return the all issuer
     */
    ResponseData<Integer> getIssuerCount();

    /**
     * get the issuer count with Recognized.
     * @return the all issuer with Recognized
     */
    ResponseData<Integer> getRecognizedIssuerCount();

    /**
     * get the issuer size in issuerType.
     * @param issuerType the issuerType
     * @return the all issuer in issuerType
     */
    ResponseData<Integer> getSpecificTypeIssuerSize(String issuerType);

    /**
     * get the issuer type count.
     * @return the all issuer type
     */
    ResponseData<Integer> getIssuerTypeCount();

    /**
     * remove the issuerType.
     * @param callerAuth the caller
     * @param issuerType the issuerType name
     * @return true is success, false is fail
     */
    ResponseData<Boolean> removeIssuerType(WeIdAuthentication callerAuth, String issuerType);

    /**
     * get the issuerType list.
     * @param index the start index
     * @param num the page size
     * @return the issuerType list
     */
    ResponseData<List<IssuerType>> getIssuerTypeList(Integer index, Integer num);
}
