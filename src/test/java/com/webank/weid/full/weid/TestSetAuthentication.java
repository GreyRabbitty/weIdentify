

package com.webank.weid.full.weid;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.common.LogUtil;
import com.webank.weid.common.PasswordKey;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.full.TestBaseService;
import com.webank.weid.full.TestBaseUtil;
import com.webank.weid.protocol.base.WeIdDocument;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.request.AuthenticationArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.util.DateUtils;

/**
 * setAuthentication method for testing WeIdService.
 *
 * @author v_wbgyang
 */
public class TestSetAuthentication extends TestBaseService {

    private static final Logger logger = LoggerFactory.getLogger(TestSetAuthentication.class);

    @Override
    public synchronized void testInit() {
        super.testInit();
    }

    /**
     * case: publicKey repeat, set failed.
     */
    @Test
    public void testSetAuthentication_setAuthenticationSuccess() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.AUTHENTICATION_PUBLIC_KEY_MULTIBASE_EXISTS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());

    }

    /**
     * case: set two Authentication success.
     */
    @Test
    public void testSetAuthentication_setTwoAuthenticationSuccess() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdNew);
        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdNew.getWeId(),
            setAuthenticationArgs,
            createWeIdNew.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);
        Assert.assertEquals(ErrorCode.AUTHENTICATION_PUBLIC_KEY_MULTIBASE_EXISTS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());

        AuthenticationArgs setAuthenticationArgs1 =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdNew);
        setAuthenticationArgs1.setController(createWeIdResult.getWeId());
        ResponseData<Boolean> response1 = weIdService.setAuthentication(
            createWeIdNew.getWeId(),
            setAuthenticationArgs1,
            createWeIdNew.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response1);
        Assert.assertEquals(ErrorCode.AUTHENTICATION_PUBLIC_KEY_MULTIBASE_EXISTS.getCode(), response1.getErrorCode().intValue());
        Assert.assertEquals(false, response1.getResult());

        ResponseData<WeIdDocument> weIdDoc =
            weIdService.getWeIdDocument(createWeIdNew.getWeId());
        LogUtil.info(logger, "setAuthentication", weIdDoc);
        Assert.assertEquals(1, weIdDoc.getResult().getAuthentication().size());
    }

    /**
     * case: WeIdentity DID is blank.
     */
    @Test
    public void testSetAuthentication_weIdBlank() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication("",
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: WeIdentity DID is bad format.
     */
    @Test
    public void testSetAuthentication_weIdFormat() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            "di:weid:0xbbd97a63365b6c9fb6b011a8d294307a3b7dac73",
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: WeIdentity DID is not exists.
     */
    @Test
    public void testSetAuthentication_weIdNotExist() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            "did:weid:0xbb1670306aedfaeb75cff9581c99e56ba4797431",
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_DOES_NOT_EXIST.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: WeIdentity DID contain zh.
     */
    @Test
    public void testSetAuthentication_weIdContainZh() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            "did:weid:你好",
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: publicKey is a new key.
     */
    @Test
    public void testSetAuthentication_newPubKey() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        PasswordKey passwordKey = TestBaseUtil.createEcKeyPair();

        setAuthenticationArgs.setPublicKey(passwordKey.getPublicKey());

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * case: publicKey is null.
     */
    @Test
    public void testSetAuthentication_PubKeyNull() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setPublicKey(null);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: publicKey is belong other weId.
     */
    @Test
    public void testSetAuthentication_PubKeyOther() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        String newPubkey = createWeIdNew.getUserWeIdPublicKey().getPublicKey();
        setAuthenticationArgs.setPublicKey(newPubkey);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * case: publicKey is invalid ("xxxxxxxxxx" or "1111111111111").
     */
    @Test
    public void testSetAuthentication_pubKeyxxx() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setPublicKey("xxxxxxxxxxxx");

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * case: publicKey contain special character.
     */
    @Test
    public void testSetAuthentication_pubKeySpecialChar() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setPublicKey("~！@#￥%……&*？》《;az09");

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: userWeIdPrivateKey is null.
     */
    @Test
    public void testSetAuthentication_priKeyNull() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs, null);
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: privateKey is invalid.
     */
    @Test
    public void testSetAuthentication_invalidPriKey() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(createWeIdResult.getWeId(),
            setAuthenticationArgs,
            new WeIdPrivateKey("xxxxxxxxxxxxxxxxxxxxx"));
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_INVALID.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: privateKey and privateKey of WeIdentity DID does not match.
     */
    @Test
    public void testSetAuthentication_newPriKey() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        PasswordKey passwordKey = TestBaseUtil.createEcKeyPair();
        setAuthenticationArgs.setPublicKey(passwordKey.getPublicKey());
        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(),
            setAuthenticationArgs,
            new WeIdPrivateKey(passwordKey.getPrivateKey()));
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: other WeIdentity DID.
     */
    @Test
    public void testSetAuthentication_otherWeid() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdNew.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_PRIVATEKEY_DOES_NOT_MATCH.getCode(),
            response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: owner is other WeIdentity DID.
     */
    @Test
    public void testSetAuthentication_ownerIsOtherWeId() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setController(createWeIdNew.getWeId());
        setAuthenticationArgs.setPublicKey(createWeIdNew.getUserWeIdPublicKey().getPublicKey());
        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * case: owner is the WeIdentity DID.
     */
    @Test
    public void testSetAuthentication_owerNotExist() {
        String weId = createWeId().getWeId();
        weId = weId.replace(weId.substring(weId.length() - 4, weId.length()),
            DateUtils.getNoMillisecondTimeStampString());
        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setController(weId);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: owner is the WeIdentity DID.
     */
    @Test
    public void testSetAuthentication_twoAuthentication() {

        final String weid1 = createWeId().getWeId();
        final String weid2 = createWeId().getWeId();
        CreateWeIdDataResult createWeIdResultWithSetAttr = super.createWeId();
        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResultWithSetAttr);
        setAuthenticationArgs.setController(weid1);
        setAuthenticationArgs.setPublicKey(createWeId().getUserWeIdPublicKey().getPublicKey());
        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResultWithSetAttr.getWeId(),
            setAuthenticationArgs,
            createWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());

        AuthenticationArgs setAuthenticationArgs1 =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResultWithSetAttr);
        setAuthenticationArgs1.setController(weid2);
        setAuthenticationArgs1.setPublicKey("12345678");
        ResponseData<Boolean> res = weIdService.setAuthentication(
            createWeIdResultWithSetAttr.getWeId(),
            setAuthenticationArgs1,
            createWeIdResultWithSetAttr.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", res);
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), res.getErrorCode().intValue());

        ResponseData<WeIdDocument> weIdDoc =
            weIdService.getWeIdDocument(createWeIdResultWithSetAttr.getWeId());
        LogUtil.info(logger, "setAuthentication", weIdDoc);
        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), weIdDoc.getErrorCode().intValue());
        Assert.assertEquals(3, weIdDoc.getResult().getAuthentication().size());
    }

    /**
     * case: owner is invalid.
     */
    @Test
    public void testSetAuthentication_invalidOwner() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setController("xxxxxxxxxxxxxxxxx");

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

    /**
     * case: owner is null.
     */
    @Test
    public void testSetAuthentication_ownerNull() {

        final CreateWeIdDataResult weId = super.createWeId();
        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(weId);
        setAuthenticationArgs.setController(null);

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());

        ResponseData<WeIdDocument> weIdDoc
            = weIdService.getWeIdDocument(weId.getWeId());
        LogUtil.info(logger, "setAuthentication", weIdDoc);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), weIdDoc.getErrorCode().intValue());
        Assert.assertEquals(1, weIdDoc.getResult().getAuthentication().size());
    }

    /**
     * case: owner is blank.
     */
    @Test
    public void testSetAuthentication_ownerBlank() {

        final CreateWeIdDataResult weId = super.createWeId();
        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setController("");
        setAuthenticationArgs.setPublicKey(weId.getUserWeIdPublicKey().getPublicKey());

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * case: owner contain special char.
     */
    @Test
    public void testSetAuthentication_ownerContainSpecialChar() {

        AuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createWeIdResult);
        setAuthenticationArgs.setController("~!@#$%^&*()——+=？》《，<>aq10");

        ResponseData<Boolean> response = weIdService.setAuthentication(
            createWeIdResult.getWeId(), setAuthenticationArgs,
            createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.WEID_INVALID.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }


    /**
     * case: setAuthenticationArgs is null.
     */
    @Test
    public void testSetAuthenticationCase21() {

        ResponseData<Boolean> response = weIdService.setAuthentication(createWeIdResult.getWeId(),
            null, createWeIdResult.getUserWeIdPrivateKey());
        LogUtil.info(logger, "setAuthentication", response);

        Assert.assertEquals(ErrorCode.ILLEGAL_INPUT.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(false, response.getResult());
    }

}
