/*
 *       Copyright© (2018) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-java-sdk.
 *
 *       weidentity-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.full;

import com.webank.weid.BaseTest;
import com.webank.weid.common.BeanUtil;
import com.webank.weid.constant.ErrorCode;
import com.webank.weid.protocol.base.CptBaseInfo;
import com.webank.weid.protocol.base.Credential;
import com.webank.weid.protocol.base.WeIdPrivateKey;
import com.webank.weid.protocol.base.WeIdPublicKey;
import com.webank.weid.protocol.request.CreateCredentialArgs;
import com.webank.weid.protocol.request.CreateWeIdArgs;
import com.webank.weid.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.protocol.request.RegisterCptArgs;
import com.webank.weid.protocol.request.SetAuthenticationArgs;
import com.webank.weid.protocol.request.SetPublicKeyArgs;
import com.webank.weid.protocol.request.SetServiceArgs;
import com.webank.weid.protocol.response.CreateWeIdDataResult;
import com.webank.weid.protocol.response.ResponseData;
import com.webank.weid.util.WeIdUtils;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;

/**
 * testing basic method classes.
 * 
 * @author v_wbgyang
 *
 */
public abstract class TestBaseServcie extends BaseTest {

    protected static List<String> issuerPrivateList = new ArrayList<String>();

    protected static boolean isInitIssuer = false;

    protected static CreateWeIdDataResult createWeId = null;

    protected static CreateWeIdDataResult createWeIdNew = null;

    protected static CreateWeIdDataResult createWeIdForGetDoc = null;

    protected static CreateWeIdDataResult createWeIdWithSetAttr = null;

    protected static CreateCredentialArgs createCredentialArgs = null;

    protected static RegisterCptArgs registerCptArgs = null;

    protected static CptBaseInfo cptBaseInfo = null;

    /** 
     * initializing related services.
     */
    @Override
    public void testInit() throws Exception {

        if (!isInitIssuer) {
            try {
                issuerPrivateList.add(TestBaseUtil.privKey);
                initIssuer("org1.txt");
                isInitIssuer = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (createWeId == null) {
            createWeId = this.createWeId();
        }
        if (createWeIdWithSetAttr == null) {
            createWeIdWithSetAttr = this.createWeIdWithSetAttr();
        }
        if (createWeIdNew == null) {
            createWeIdNew = this.createWeId();
        }
        if (createWeIdForGetDoc == null) {
            createWeIdForGetDoc = this.createWeIdWithSetAttr();
        }
    }

    /**
     * according to the analysis of the private key to create weIdentity DID,and
     * registered as an authority, and its private key is recorded.
     * 
     * @param fileName fileName
     * @throws Exception  may be throw Exception
     */
    private void initIssuer(String fileName) throws Exception {

        String[] pk = TestBaseUtil.resolvePk(fileName);

        CreateWeIdArgs createWeIdArgs1 = TestBaseUtil.buildCreateWeIdArgs();
        createWeIdArgs1.setPublicKey(pk[0]);
        createWeIdArgs1.getWeIdPrivateKey().setPrivateKey(pk[1]);
        ResponseData<String> response1 = weIdService.createWeId(createWeIdArgs1);
        if (response1.getErrorCode().intValue() != ErrorCode.WEID_ALREADY_EXIST.getCode()
            && response1.getErrorCode().intValue() != ErrorCode.SUCCESS.getCode()) {
            throw new RuntimeException(response1.getErrorMessage());
        }

        String weId = WeIdUtils.convertPublicKeyToWeId(pk[0]);

        CreateWeIdDataResult createResult = new CreateWeIdDataResult();
        createResult.setWeId(weId);
        createResult.setUserWeIdPrivateKey(new WeIdPrivateKey());
        createResult.setUserWeIdPublicKey(new WeIdPublicKey());
        createResult.getUserWeIdPrivateKey().setPrivateKey(pk[1]);
        createResult.getUserWeIdPublicKey().setPublicKey(pk[0]);

        this.setPublicKey(createResult, pk[0], createResult.getWeId());
        this.setAuthentication(createResult, pk[0], createResult.getWeId());

        CreateWeIdDataResult createWeId = new CreateWeIdDataResult();
        createWeId.setWeId(weId);

        RegisterAuthorityIssuerArgs registerAuthorityIssuerArgs =
            TestBaseUtil.buildRegisterAuthorityIssuerArgs(createWeId);

        ResponseData<Boolean> response =
            authorityIssuerService.registerAuthorityIssuer(registerAuthorityIssuerArgs);
        System.out.println("\nregisterAuthorityIssuer result:");
        BeanUtil.print(response);

        if (response.getErrorCode()
            .intValue() != ErrorCode.AUTHORITY_ISSUER_CONTRACT_ERROR_ALREADY_EXIST.getCode()
            && response.getErrorCode().intValue() != ErrorCode.SUCCESS.getCode()) {
            throw new RuntimeException(response1.getErrorMessage());
        }

        issuerPrivateList.add(pk[1]);
        System.out.println("initIssuer success");
    }

    /**
     * verifyCredential.
     * 
     * @param credential credential
     * @return
     */
    protected ResponseData<Boolean> verifyCredential(Credential credential) {

        ResponseData<Boolean> response = credentialService.verifyCredential(credential);
        System.out.println("\nverifyCredentialWithSpecifiedPubKey result:");
        BeanUtil.print(response);

        return response;
    }

    /**
     * createCredential.
     * 
     * @param createCredentialArgs createCredentialArgs
     * @return
     */
    protected Credential createCredential(CreateCredentialArgs createCredentialArgs) {

        ResponseData<Credential> response =
            credentialService.createCredential(createCredentialArgs);
        System.out.println("\ncreateCredential result:");
        BeanUtil.print(response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());

        return response.getResult();
    }

    /**
     * cpt register.
     * 
     * @param createWeId  createWeId
     * @param registerCptArgs registerCptArgs
     * @param isRegisterAuthorityIssuer isRegisterAuthorityIssuer
     * @return
     */
    protected CptBaseInfo registerCpt(
        CreateWeIdDataResult createWeId,
        RegisterCptArgs registerCptArgs) {

        ResponseData<CptBaseInfo> response = cptService.registerCpt(registerCptArgs);
        System.out.println("\nregisterCpt result:");
        BeanUtil.print(response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertNotNull(response.getResult());

        return response.getResult();
    }

    /**
     * cpt register.
     * 
     * @param createWeId createWeId
     * @param isRegisterAuthorityIssuer isRegisterAuthorityIssuer
     * @return
     */
    protected CptBaseInfo registerCpt(CreateWeIdDataResult createWeId) {

        RegisterCptArgs registerCptArgs = TestBaseUtil.buildRegisterCptArgs(createWeId);

        CptBaseInfo cptBaseInfo = registerCpt(createWeId, registerCptArgs);

        return cptBaseInfo;
    }

    /**
     * create weIdentity DID and registerAuthorityIssuer.
     * 
     * @return CreateWeIdDataResult
     * @throws Exception may be throw Exception
     */
    protected CreateWeIdDataResult registerAuthorityIssuer() throws Exception {

        CreateWeIdDataResult createWeId = this.createWeId();

        registerAuthorityIssuer(createWeId);

        return createWeId;
    }

    /** 
     * registerAuthorityIssuer default.
     */
    protected void registerAuthorityIssuer(CreateWeIdDataResult createWeId) {

        RegisterAuthorityIssuerArgs registerAuthorityIssuerArgs =
            TestBaseUtil.buildRegisterAuthorityIssuerArgs(createWeId);

        ResponseData<Boolean> response =
            authorityIssuerService.registerAuthorityIssuer(registerAuthorityIssuerArgs);
        System.out.println("\nregisterAuthorityIssuer result:");
        BeanUtil.print(response);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), response.getErrorCode().intValue());
        Assert.assertEquals(true, response.getResult());
    }

    /**
     * create weIdentity DID and set Attribute default.
     *
     * @return CreateWeIdDataResult
     */
    protected CreateWeIdDataResult createWeIdWithSetAttr() {

        CreateWeIdDataResult createWeId = this.createWeId();

        try {
            this.setPublicKey(createWeId,
                createWeId.getUserWeIdPublicKey().getPublicKey(),
                createWeId.getWeId());
            this.setAuthentication(createWeId,
                createWeId.getUserWeIdPublicKey().getPublicKey(),
                createWeId.getWeId());
            this.setService(createWeId, TestData.serviceType, TestData.serviceEndpoint);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(false, true);
        }
        return createWeId;
    }

    /**
     * create weIdentity DID without set Attribute default.
     * 
     * @return CreateWeIdDataResult
     */
    protected CreateWeIdDataResult createWeId() {

        ResponseData<CreateWeIdDataResult> createWeIdDataResult = weIdService.createWeId();
        System.out.println("\ncreateWeId result:");
        BeanUtil.print(createWeIdDataResult);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(),
            createWeIdDataResult.getErrorCode().intValue());
        Assert.assertNotNull(createWeIdDataResult.getResult());

        return createWeIdDataResult.getResult();
    }

    /**
     * setPublicKey default.
     * 
     * @param createResult createResult
     * @param publicKey publicKey
     * @param owner owner
     * @throws Exception Exception
     */
    protected void setPublicKey(CreateWeIdDataResult createResult, String publicKey, String owner)
        throws Exception {

        // setPublicKey for this WeId
        SetPublicKeyArgs setPublicKeyArgs = TestBaseUtil.buildSetPublicKeyArgs(createResult);
        setPublicKeyArgs.setPublicKey(publicKey);
        setPublicKeyArgs.setOwner(owner);

        ResponseData<Boolean> responseSetPub = weIdService.setPublicKey(setPublicKeyArgs);
        System.out.println("\nsetPublicKey result:");
        BeanUtil.print(responseSetPub);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseSetPub.getErrorCode().intValue());
        Assert.assertEquals(true, responseSetPub.getResult());
    }

    /**
     * setService default.
     * 
     * @param createResult createResult
     * @param serviceType serviceType
     * @param serviceEnpoint serviceEnpoint
     * @throws Exception may be throw Exception
     */
    protected void setService(
        CreateWeIdDataResult createResult,
        String serviceType,
        String serviceEnpoint)
        throws Exception {

        // setService for this weIdentity DID
        SetServiceArgs setServiceArgs = TestBaseUtil.buildSetServiceArgs(createResult);
        setServiceArgs.setType(serviceType);
        setServiceArgs.setServiceEndpoint(serviceEnpoint);

        ResponseData<Boolean> responseSetSer = weIdService.setService(setServiceArgs);
        System.out.println("\nsetService result:");
        BeanUtil.print(responseSetSer);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseSetSer.getErrorCode().intValue());
        Assert.assertEquals(true, responseSetSer.getResult());
    }

    /**
     * setAuthenticate default.
     * 
     * @param createResult createResult
     * @param publicKey publicKey
     * @param owner owner
     * @throws Exception may be throw Exception
     */
    protected void setAuthentication(
        CreateWeIdDataResult createResult,
        String publicKey,
        String owner)
        throws Exception {

        // setAuthenticate for this weIdentity DID
        SetAuthenticationArgs setAuthenticationArgs =
            TestBaseUtil.buildSetAuthenticationArgs(createResult);
        setAuthenticationArgs.setOwner(owner);
        setAuthenticationArgs.setPublicKey(publicKey);
        ResponseData<Boolean> responseSetAuth =
            weIdService.setAuthentication(setAuthenticationArgs);
        System.out.println("\nsetAuthentication result:");
        BeanUtil.print(responseSetAuth);

        Assert.assertEquals(ErrorCode.SUCCESS.getCode(), responseSetAuth.getErrorCode().intValue());
        Assert.assertEquals(true, responseSetAuth.getResult());
    }
}
