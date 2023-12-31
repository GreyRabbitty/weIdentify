

package com.webank.weid.full.auth;

import com.webank.weid.util.DataToolUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.common.LogUtil;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.full.TestBaseService;
import com.webank.weid.full.TestBaseUtil;
import com.webank.weid.protocol.base.AuthorityIssuer;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.RemoveAuthorityIssuerArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;

import java.math.BigInteger;

/**
 * isAuthorityIssuer method for testing AuthorityIssuerService.
 *
 * @author v_wbgyang
 */
public class TestIsAuthorityIssuer extends TestBaseService {

    private static final Logger logger = LoggerFactory.getLogger(TestIsAuthorityIssuer.class);

    private static CreateWeIdDataResult createWeId;

    @Override
    public synchronized void testInit() {

        super.testInit();
        if (createWeId == null) {
            createWeId = super.registerAuthorityIssuer();
        }

    }

    /**
     * case: is authority issuer success .
     */
    //@Test
    public void testIsAuthorityIssuerSuccess() {

        ResponseData<Boolean> response =
            authorityIssuerService.isAuthorityIssuer(createWeId.getWeId());
        LogUtil.info(logger, "isAuthorityIssuer", response);
        Assert.assertFalse(response.getResult());
        AuthorityIssuer authorityIssuer =
            authorityIssuerService.queryAuthorityIssuerInfo(createWeId.getWeId()).getResult();
        Assert.assertFalse(authorityIssuer.isRecognized());
        response = authorityIssuerService.recognizeAuthorityIssuer(createWeId.getWeId(),
            new WeIdPrivateKey(privateKey));
        Assert.assertTrue(response.getResult());
        response = authorityIssuerService.isAuthorityIssuer(createWeId.getWeId());
        Assert.assertTrue(response.getResult());
        authorityIssuer =
            authorityIssuerService.queryAuthorityIssuerInfo(createWeId.getWeId()).getResult();
        Assert.assertFalse(authorityIssuer.isRecognized());
        authorityIssuerService
            .deRecognizeAuthorityIssuer(createWeId.getWeId(), new WeIdPrivateKey(privateKey));
        response = authorityIssuerService.isAuthorityIssuer(createWeId.getWeId());
        Assert.assertFalse(response.getResult());

        response = authorityIssuerService.recognizeAuthorityIssuer(
            createWeIdWithSetAttr().getWeId(), new WeIdPrivateKey(privateKey));
        Assert.assertFalse(response.getResult());
        Assert.assertEquals(response.getErrorCode().intValue(),
            ErrorCode.AUTHORITY_ISSUER_CONTRACT_ERROR_NOT_EXISTS.getCode());

        authorityIssuerService.recognizeAuthorityIssuer(createWeId.getWeId(),
            new WeIdPrivateKey(privateKey));
        response = authorityIssuerService.deRecognizeAuthorityIssuer(createWeId.getWeId(),
            new WeIdPrivateKey("11111111"));
        Assert.assertFalse(response.getResult());
        Assert.assertEquals(response.getErrorCode().intValue(),
            ErrorCode.CONTRACT_ERROR_NO_PERMISSION.getCode());
    }

    /**
     * case: WeIdentity DID is bad format.
     */
    @Test
    public void testIsAuthorityIssuerWeIdFormat() {

        ResponseData<Boolean> response = authorityIssuerService.isAuthorityIssuer("as~12345678>?<");
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is not exists.
     */
    @Test
    public void testIsAuthorityIssuerWeIdSorted() {

        String weId = "weid:did:0x5f3d8234e93823fac7ebdf0cfaa03b6a43d87733";
        ResponseData<Boolean> response = authorityIssuerService
            .isAuthorityIssuer(weId);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is not start with 0x.
     */
    @Test
    public void testIsAuthorityIssuerWeIdnotHex() {

        String weId = "weid:did:00f3d8234e93823fac7ebdf0cfaa03b6a43d87733";
        ResponseData<Boolean> response = authorityIssuerService
            .isAuthorityIssuer(weId);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is not start with space and end with space.
     */
    @Test
    public void testIsAuthorityIssuerWeIdContainSpace() {

        String weId = "weid:did:0xf3d8234e93823fac7ebdf0cfaa03b6a43d87733";
        ResponseData<Boolean> response = authorityIssuerService
            .isAuthorityIssuer(weId);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: WeIdentity DID is null.
     */
    @Test
    public void testIsAuthorityIssuerWeIdNull() {

        ResponseData<Boolean> response = authorityIssuerService.isAuthorityIssuer(null);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: WeIdentity DID is blank.
     */
    @Test
    public void testIsAuthorityIssuerWeIdBlank() {

        ResponseData<Boolean> response = authorityIssuerService.isAuthorityIssuer("");
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is registed by other.
     */
    @Test
    public void testIsAuthorityIssuerWeIdNotRegister() {

        String weId = createWeId().getWeId();
        ResponseData<Boolean> response = authorityIssuerService
            .isAuthorityIssuer(weId);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is not exists.
     */
    @Test
    public void testIsAuthorityIssuerWeIdNotExist() {

        ResponseData<Boolean> response = authorityIssuerService
            .isAuthorityIssuer("did:weid:0x5f3d8234e93823fac7ebdf0cfaa03b6a43d87733");
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: the WeIdentity DID is removed.
     */
    @Test
    public void testIsAuthorityIssuerRemovedWeId() {

        CreateWeIdDataResult createWeId = super.registerAuthorityIssuer();
        LogUtil.info(logger, "registerAuthorityIssuer", createWeId);

        RemoveAuthorityIssuerArgs removeAuthorityIssuerArgs =
            TestBaseUtil.buildRemoveAuthorityIssuerArgs(createWeId, privateKey);

        ResponseData<Boolean> response =
            authorityIssuerService.removeAuthorityIssuer(removeAuthorityIssuerArgs);
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());

        response = authorityIssuerService.isAuthorityIssuer(createWeId.getWeId());
        LogUtil.info(logger, "isAuthorityIssuer", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

}
