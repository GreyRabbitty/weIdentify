

package com.webank.weid.full.cpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webank.weid.common.LogUtil;
import com.webank.weid.common.PasswordKey;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.constant.JsonSchemaConstant;
import com.webank.weid.full.TestBaseService;
import com.webank.weid.full.TestBaseUtil;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.base.WeIdAuthentication;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.CptMapArgs;
import com.webank.weid.protocol.request.CptStringArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.util.WeIdUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * registerCpt(CptMapArgs args, Integer cptId) method for testing CptService.
 *
 * @author v_wbgyang.
 */
public class TestRegisterCptArgsWithId extends TestBaseService {

    private static final Logger logger = LoggerFactory.getLogger(TestRegisterCptArgsWithId.class);

    private static CreateWeIdDataResult createWeId = null;

    @Override
    public synchronized void testInit() {

        super.testInit();
        if (createWeId == null) {
            createWeId = super.createWeId();
            super.registerAuthorityIssuer(createWeId);
            ResponseData<Boolean> responseData = authorityIssuerService
                .recognizeAuthorityIssuer(createWeId.getWeId(), new WeIdPrivateKey(privateKey));
        }
    }

    /**
     * case: when ctpId in [1,999],no permission register cpt.
     */
    @Test
    public void testRegisterCptArgsWithId_noPermission() {
        // The "system" CPT ID
        Random rand = new Random();
        Integer keyCptId = rand.nextInt(999) + 1;
        CptMapArgs registerCptArgs = TestBaseUtil.buildCptArgs(createWeId);
        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, keyCptId);
        LogUtil.info(logger, "registerCpt", response);
        Assert.assertEquals(ErrorCode.CPT_NO_PERMISSION.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case: when ctpId bigger>200 000 0,register ordinary cpt success.
     */
    // CI hold: @Test
    public void testRegisterCptArgsWithId_ordinaryCptSuccess() throws Exception {

        CreateWeIdDataResult weIdResult = super.createWeId();
        CptMapArgs registerCptArgs =
            TestBaseUtil.buildCptArgs(weIdResult);

        Integer issuerCptId = 2000000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, issuerCptId);
        LogUtil.info(logger, "testRegisterCptArgs with cptid", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());
    }

    /**
     * case: when ctpId in [1000,2000000],but not register Authority Issuer ,then register cpt
     * fail.
     */
    @Test
    public void testRegisterCptArgsWithId_registerCptFail() throws Exception {

        CreateWeIdDataResult weIdResult = super.createWeId();
        CptMapArgs registerCptArgs =
            TestBaseUtil.buildCptArgs(weIdResult);

        Integer issuerCptId = 1000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, issuerCptId);
        LogUtil.info(logger, "testRegisterCptArgs with cptid", response);

        Assert.assertEquals(ErrorCode.CPT_NO_PERMISSION.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case: when ctpid in [1000,2000000],register auth cpt success.
     */
    @Test
    public void testRegisterCptArgsWithId_registerAuthCptSuccess() throws Exception {

        CptMapArgs registerCptArgs =
            TestBaseUtil.buildCptArgs(createWeId);

        Integer issuerCptId = 1000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, issuerCptId);
        LogUtil.info(logger, "testRegisterCptArgs with cptid", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());
    }

    /**
     * case: when ctpid in [2000000,],register auth cpt success.
     */
    @Test
    public void testRegisterCptArgsWithId_registerAuthCptFail() throws Exception {

        CptMapArgs registerCptArgs =
            TestBaseUtil.buildCptArgs(createWeId);

        Integer issuerCptId = 2000000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 50 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, issuerCptId);
        LogUtil.info(logger, "testRegisterCptArgs with cptid", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());
    }


    /**
     * case: register cpt id success. Query multiple times until find an available id, register
     * successfully, and retry with an expected failure.
     */
    @Test
    public void testRegisterCptArgsWithId_repeat() {
        Integer cptId = 6000000;
        // Add randomness in the next available cpt number - also for faster test cycles
        while (cptService.queryCpt(cptId).getResult() != null) {
            cptId += (int) (Math.random() * 50 + 1);
        }
        CptMapArgs registerCptArgs = TestBaseUtil.buildCptArgs(createWeId);
        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);
        Assert.assertEquals(response.getErrorCode().intValue(), ErrorCode.SUCCESS.getCode());
        Assert.assertNotNull(response.getResult());

        // do it twice
        ResponseData<CptBaseInfo> responseData = cptService.registerCpt(registerCptArgs, cptId);
        LogUtil.info(logger, "registerCpt", responseData);
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.CPT_ALREADY_EXIST.getCode());
        Assert.assertNull(responseData.getResult());
    }

    /**
     * case： cptPublisher is not exists and the private key does not match.
     */
    @Test
    public void testRegisterCptArgsWithId_weIdNotExist() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication()
            .setWeId("did:weid:0xbb1670306aedfaeb75cff9581c99e56ba4797431");

        Integer cptId = 50000;
        while (cptService.queryCpt(cptId).getResult() != null) {
            cptId += (int) (Math.random() * 50 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： registerCptArgs is null.
     */
    @Test
    public void testRegisterCptArgsWithId_CptArgsNull() {

        CptMapArgs cptMapArgs = null;
        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(), response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptJsonSchema is null.
     */
    @Test
    public void testRegisterCptArgsWithId_cptJsonSchemaNull() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.setCptJsonSchema(null);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.CPT_JSON_SCHEMA_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptJsonSchema is bad format.
     */
    @Test
    public void testRegisterCptArgsWithId_cptJsonSchema() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        HashMap<String, Object> cptJsonSchema = new HashMap<>();
        cptMapArgs.setCptJsonSchema(cptJsonSchema);
        cptJsonSchema.put("name", "rocky xia is good man");
        cptJsonSchema.put("年龄", 18);
        cptJsonSchema.put("account", 192.5);

        Integer issuerCptId = 20000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }

        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, issuerCptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());
    }

    /**
     * case： cptJsonSchema too long.
     */
    @Test
    public void testRegisterCptArgsWithIdCase5() throws JsonProcessingException, IOException {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);

        StringBuffer value = new StringBuffer("");
        for (int i = 0; i < 5000; i++) {
            value.append("x");
        }

        HashMap<String, Object> cptJsonSchema = TestBaseUtil.buildCptJsonSchema();
        cptJsonSchema.put(JsonSchemaConstant.TITLE_KEY, value.toString());
        cptMapArgs.setCptJsonSchema(cptJsonSchema);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.CPT_JSON_SCHEMA_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisher is blank.
     */
    @Test
    public void testRegisterCptArgsWithId_cptPublisherNull() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().setWeId(null);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisher is blank.
     */
    @Test
    public void testRegisterCptArgsWithId_cptPublisherBlank() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().setWeId("");

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisher is invalid.
     */
    @Test
    public void testRegisterCptArgsWithId_invalidCptPublisher() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().setWeId("did:weid:0x!@#$%^&*()-+?.,中国");

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisherPrivateKey is null.
     */
    @Test
    public void testRegisterCptArgsWithId_priKeyNull() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().setWeIdPrivateKey(null);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： privateKey is null.
     */
    @Test
    public void testRegisterCptArgsWithId_setPriKeyNull() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().getWeIdPrivateKey().setPrivateKey(null);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisherPrivateKey is null.
     */
    @Test
    public void testRegisterCptArgsWithId_priKeyBlank() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().getWeIdPrivateKey().setPrivateKey("");

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： privateKey is invalid.
     */
    @Test
    public void testRegisterCptArgsWithId_invalidPriKey() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().getWeIdPrivateKey()
            .setPrivateKey("123~!@#$%^&*()-+=？》《中国OIU");

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： privateKey is new privateKey.
     */
    @Test
    public void testRegisterCptArgsWithId_newPriKey() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().getWeIdPrivateKey()
            .setPrivateKey(TestBaseUtil.createEcKeyPair().getPrivateKey());

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： privateKey is SDK privateKey.
     */
    @Test
    public void testRegisterCptArgsWithId_sdkPriKey() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().getWeIdPrivateKey().setPrivateKey(privateKey);

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： cptPublisher is not exists and the private key is match.
     */
    @Test
    public void testRegisterCptArgsWithId_cptPublisherNotExist() {

        PasswordKey passwordKey = TestBaseUtil.createEcKeyPair();
        String weId = WeIdUtils.convertPublicKeyToWeId(passwordKey.getPublicKey());

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.getWeIdAuthentication().setWeId(weId);
        cptMapArgs.getWeIdAuthentication()
            .getWeIdPrivateKey()
            .setPrivateKey(passwordKey.getPrivateKey());

        Integer cptId = 50000;
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(ErrorCode.CPT_PUBLISHER_NOT_EXIST.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： WeIdAuthentication is null.
     */
    @Test
    public void testRegisterCptArgsWithId_weIdAuthenticationNull() {

        CptMapArgs cptMapArgs = TestBaseUtil.buildCptArgs(createWeId);
        cptMapArgs.setWeIdAuthentication(null);

        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(
            ErrorCode.WEID_AUTHORITY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case： WeIdAuthentication is blank.
     */
    @Test
    public void testRegisterCptArgsWithId_weIdAuthenticationBlank() {

        WeIdAuthentication weIdAuthentication = new WeIdAuthentication();
        CptMapArgs cptMapArgs = new CptMapArgs();
        cptMapArgs.setWeIdAuthentication(weIdAuthentication);
        cptMapArgs.setCptJsonSchema(new HashMap<>());

        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptMapArgs);
        LogUtil.info(logger, "registerCpt", response);

        Assert.assertEquals(
            ErrorCode.WEID_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());
    }

    /**
     * case: register cpt id w/ and w/o permission.
     */
    @Test
    public void testRegisterCptArgsWithIdPermission() {
        // The "system" CPT ID
        Integer keyCptId = 50;
        CptMapArgs registerCptArgs = TestBaseUtil.buildCptArgs(createWeId);
        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, keyCptId);
        LogUtil.info(logger, "registerCpt", response);
        Assert.assertEquals(ErrorCode.CPT_NO_PERMISSION.getCode(),
            response.getErrorCode().intValue());
        Assert.assertNull(response.getResult());

        // The authority issuer related cpt ID
        Integer issuerCptId = 1200000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }
        ResponseData<CptBaseInfo> responseData = cptService
            .registerCpt(registerCptArgs, issuerCptId);
        LogUtil.info(logger, "registerCpt", responseData);
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseData.getErrorCode().intValue());
        Assert.assertNotNull(responseData.getResult());

        ResponseData<CptBaseInfo> errResponse = cptService.registerCpt(registerCptArgs, null);
        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(), errResponse.getErrorCode().intValue());
    }

    /**
     * case: register cpt id success. Query multiple times until find an available id, register
     * successfully, and retry with an expected failure.
     */
    @Test
    public void testRegisterCptWithIdSuccessAndDuplicate() {
        Integer cptId = 6000000;
        // Add randomness in the next available cpt number - also for faster test cycles
        while (cptService.queryCpt(cptId).getResult() != null) {
            cptId += (int) (Math.random() * 50 + 1);
        }
        CptMapArgs registerCptArgs = TestBaseUtil.buildCptArgs(createWeId);
        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs, cptId);
        LogUtil.info(logger, "registerCpt", response);
        Assert.assertEquals(response.getErrorCode().intValue(), ErrorCode.SUCCESS.getCode());
        Assert.assertNotNull(response.getResult());

        // do it twice
        ResponseData<CptBaseInfo> responseData = cptService.registerCpt(registerCptArgs, cptId);
        LogUtil.info(logger, "registerCpt", responseData);
        Assert.assertEquals(responseData.getErrorCode().intValue(),
            ErrorCode.CPT_ALREADY_EXIST.getCode());
        Assert.assertNull(responseData.getResult());
    }

    /**
     * case: register cpt id with string args.
     */
    @Test
    public void testRegisterCptStringWithId() throws Exception {
        Integer issuerCptId = 1000000;
        while (cptService.queryCpt(issuerCptId).getResult() != null) {
            issuerCptId += (int) (Math.random() * 10 + 1);
        }
        CptStringArgs cptStringArgs =
            TestBaseUtil.buildCptStringArgs(createWeId, false);
        ResponseData<CptBaseInfo> response = cptService.registerCpt(cptStringArgs, issuerCptId);
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());

        ResponseData<CptBaseInfo> responseData = cptService.registerCpt(cptStringArgs, null);
        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(),
            responseData.getErrorCode().intValue());
    }
}
